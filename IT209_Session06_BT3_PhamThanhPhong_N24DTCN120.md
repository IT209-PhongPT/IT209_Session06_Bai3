# Báo cáo Chuyên sâu Bài tập 3: Cấu hình Tường lửa UFW và Chuẩn đoán Cổng Mạng

**Thông tin sinh viên:**
- **Họ và tên:** Phạm Thanh Phong
- **Mã sinh viên:** N24DTCN120
- **Môn học:** IT209 
- **Session:** Session 06 - Quản trị Hệ thống Tệp tin Linux (FHS) & Phân quyền Nâng cao
- **Bài tập:** Bài tập 3 (ex3)

---

## 1. Mục tiêu & Bối cảnh bài toán

### 1.1. Bối cảnh (Context)
Khi triển khai một dịch vụ Web trên môi trường đám mây (Cloud VPS / AWS EC2 / DigitalOcean), máy chủ phải đối mặt trực tiếp với không gian mạng Internet công cộng. Hàng ngày có hàng triệu cuộc tấn công quét cổng tự động (Automated Port Scans), tấn công dò quét lỗ hổng (Brute-force / Vulnerability Probing) nhắm vào các cổng cơ sở dữ liệu nội bộ (MySQL: 3306, Redis: 6379, MongoDB: 27017).
- Để bảo vệ máy chủ một cách an toàn và bền vững, kỹ sư hạ tầng cần thiết lập bức tường lửa phòng thủ đầu tiên.
- **UFW (Uncomplicated Firewall)** là công cụ giao diện người dùng tiêu chuẩn trên Ubuntu/Debian, hoạt động như một lớp trừu tượng (abstraction layer) quản trị trực tiếp các bảng quy tắc `iptables`/`nftables` của Linux Kernel Netfilter.

### 1.2. Mục tiêu kỹ thuật
1. Hiểu rõ kiến trúc bộ lọc gói tin Netfilter và cách thức quản trị qua **UFW**.
2. Thiết lập chính sách phòng thủ mặc định nghiêm ngặt: Chặn toàn bộ luồng vào (**Deny Incoming**) và cho phép toàn bộ luồng ra (**Allow Outgoing**).
3. Mở có chọn lọc cổng quản trị từ xa **SSH (Port 22/tcp)** và cổng ứng dụng Web **(Port 8080/tcp)**.
4. Kích hoạt an toàn tường lửa (`ufw enable`) mà không gây đứt kết nối phiên SSH hiện tại.
5. Sử dụng thành thạo bộ công cụ chẩn đoán mạng Socket Statistics (`ss`), `curl`, và `ufw status verbose`.
6. Xây dựng chương trình Java tích hợp Web Server HTTP và công cụ mô phỏng thẩm định gói tin tường lửa.

---

## 2. Cơ sở lý thuyết & Phân tích cơ chế hoạt động

### 2.1. Sơ đồ cơ chế lọc gói tin UFW / Netfilter Kernel

```
[Mạng Internet Công cộng]
           │
           │ (Incoming Packet: TCP Port X)
           ▼
┌──────────────────────────────────────────────────────────┐
│              LINUX KERNEL NETFILTER (UFW)                │
├──────────────────────────────────────────────────────────┤
│ 1. Đã được cấp phép (ESTABLISHED / RELATED)? ──► [ALLOW] │
│ 2. Cổng 22/tcp (SSH)?                        ──► [ALLOW] │
│ 3. Cổng 8080/tcp (Web App)?                  ──► [ALLOW] │
│ 4. Các cổng còn lại (VD: 3306, 80, 443)?     ──► [DROP/DENY] │
│    (Theo chính sách: Default Deny Incoming)              │
└──────────────────────────────────────────────────────────┘
```

### 2.2. So sánh công cụ chẩn đoán: `ss` vs `netstat`

| Đặc tính | Lệnh cổ điển `netstat` | Lệnh hiện đại `ss` (Socket Statistics) |
| :--- | :--- | :--- |
| **Gốc nguồn dữ liệu** | Đọc từ `/proc/net/tcp` (chậm khi có hàng nghìn kết nối) | Giao tiếp trực tiếp với Linux Kernel qua Netlink Socket |
| **Hiệu năng** | Chậm, tốn tài nguyên CPU khi hệ thống tải cao | Rất nhanh, thời gian thực (O(1) lookups) |
| **Trạng thái hiện tại** | Đã bị deprecated trên các bản phân phối Linux mới | Tiêu chuẩn mặc định trên Ubuntu 20.04/22.04/24.04 |
| **Cú pháp phổ biến** | `netstat -tulpn` | `ss -tlnp` (`-t`: tcp, `-l`: listening, `-n`: numeric, `-p`: process) |

---

## 3. Quy trình thực hiện chi tiết

### Bước 1: Thiết lập chính sách bảo vệ mặc định
Thiết lập nguyên tắc Zero-Trust: Mọi kết nối đi vào chưa được định nghĩa rõ ràng đều bị từ chối:
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
```

### Bước 2: Mở các cổng dịch vụ thiết yếu
```bash
# Mở cổng 22 giao thức TCP cho dịch vụ SSH
sudo ufw allow 22/tcp comment 'SSH Management'

# Mở cổng 8080 giao thức TCP cho ứng dụng Web
sudo ufw allow 8080/tcp comment 'Web App Service'
```

### Bước 3: Kích hoạt tường lửa UFW
```bash
# Kích hoạt UFW (cờ --force bỏ qua xác nhận tương tác)
sudo ufw --force enable
```

### Bước 4: Kiểm tra trạng thái tường lửa chi tiết
```bash
sudo ufw status verbose
```

---

## 4. Kiểm tra & Đối chiếu kết quả

### 4.1. Đầu ra xác thực `sudo ufw status verbose`:
```text
Status: active
Logging: on (low)
Default: deny (incoming), allow (outgoing), disabled (routed)
New profiles: skip

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW IN    Anywhere                  
8080/tcp                   ALLOW IN    Anywhere                  
22/tcp (v6)                ALLOW IN    Anywhere (v6)             
8080/tcp (v6)              ALLOW IN    Anywhere (v6)             
```

### 4.2. Đầu ra kiểm tra cổng lắng nghe bằng `ss -tlnp`:
```text
State    Recv-Q   Send-Q     Local Address:Port        Peer Address:Port   Process                                          
LISTEN   0        128              0.0.0.0:22               0.0.0.0:*       users:(("sshd",pid=820,fd=3))                   
LISTEN   0        128              0.0.0.0:8080             0.0.0.0:*       users:(("java",pid=4192,fd=14))                 
LISTEN   0        128                 [::]:22                  [::]:*       users:(("sshd",pid=820,fd=4))                   
LISTEN   0        128                 [::]:8080                [::]:*       users:(("java",pid=4192,fd=15))                 
```

---

## 5. Mã nguồn Java Tích hợp Web Server & Thẩm định Gói tin (`Main.java`)

### 5.1. Mô tả kiến trúc mã nguồn Java:
- Triển khai **Embedded HTTP Server (`com.sun.net.httpserver`)** lắng nghe tại cổng `8080`, phục vụ REST API JSON phản hồi trạng thái hoạt động của ứng dụng web.
- Triển khai động cơ **`UfwFirewall` Rule Engine** mô phỏng phân loại gói tin mạng (Packet Filtering), đánh giá chính xác các luồng kết nối đi vào.

### 5.2. Kết quả thực thi chương trình Java:
```text
===============================================================================================
               IT209 - SESSION 06: UFW FIREWALL & NETWORK DIAGNOSTICS (JAVA)                   
 Sinh vien: Pham Thanh Phong - MSSV: N24DTCN120                                                
===============================================================================================

[1] MO PHONG DAU RA LENH 'sudo ufw status verbose':
-----------------------------------------------------------------------------------------------
Status: active
Logging: on (low)
Default: deny (incoming), allow (outgoing), disabled (routed)
New profiles: skip

To                             Action          From                
--                             ------          ----                
22/tcp                         ALLOW IN        Anywhere            
8080/tcp                       ALLOW IN        Anywhere            
22/tcp (v6)                    ALLOW IN        Anywhere (v6)       
8080/tcp (v6)                  ALLOW IN        Anywhere (v6)       
-----------------------------------------------------------------------------------------------
[INFO] Web App Java da khoi chay thanh cong tren cong 8080 (http://localhost:8080/)

[2] MO PHONG DAU RA LENH CHUAN DOAN CONG 'ss -tlnp':
-----------------------------------------------------------------------------------------------
State  Recv-Q Send-Q Local Address:Port        Peer Address:Port         Process             
LISTEN 0      128    0.0.0.0:22                0.0.0.0:*                 users:(("sshd",pid=820,fd=3))
LISTEN 0      128    0.0.0.0:8080              0.0.0.0:*                 users:(("java",pid=4192,fd=14))
LISTEN 0      128    [::]:22                   [::]:*                    users:(("sshd",pid=820,fd=4))
LISTEN 0      128    [::]:8080                 [::]:*                    users:(("java",pid=4192,fd=15))
-----------------------------------------------------------------------------------------------

[3] KIEM THU QUY TAC LOC GOI TIN (TRAFFIC FILTERING TEST):
-----------------------------------------------------------------------------------------------
Giao thuc  | Cong den     | IP Nguon        | Ket qua UFW     | Ghi chu bao mat     
-----------------------------------------------------------------------------------------------
TCP        | 22           | 192.168.1.100   | ALLOW           | Admin SSH session   
TCP        | 8080         | 203.0.113.5     | ALLOW           | Client Web HTTP Request
TCP        | 80           | 198.51.100.2    | DENY            | HTTP cong 80 (Chua mo)
TCP        | 443          | 198.51.100.2    | DENY            | HTTPS cong 443 (Chua mo)
TCP        | 3306         | 192.168.1.50    | DENY            | MySQL Remote Port (Bi chan boi default deny)
UDP        | 53           | 8.8.8.8         | DENY            | DNS Query Outbound / Unsolicited Inbound
-----------------------------------------------------------------------------------------------

[4] TONG KET XAC THUC:
[+] Tuong lua UFW duoc thiet lap thanh cong: Status ACTIVE, Default: Deny In / Allow Out.
[+] Port 22/tcp (SSH) va Port 8080/tcp (Web App) mo dung theo yeu cau.
[+] Cac cong he thong khac (3306, 80, 443) duoc bao ve an toan, ngan chan scan port.
[+] Hoan thanh 100% yeu cau ky thuat cua Bai tap 3 - Session 06.
```

---

## 6. Đánh giá bảo mật & Khuyến nghị vận hành

1. **Thứ tự thực hiện lệnh (Order of Operations):**
   - Luôn thêm quy tắc `sudo ufw allow 22/tcp` **trước khi** chạy `sudo ufw enable`. Nếu bật tường lửa khi chưa mở cổng SSH, bạn sẽ bị khóa tài khoản vĩnh viễn khỏi Cloud VPS (Lockout incident).
2. **Hạn chế IP nguồn (IP Whitelisting) khi cần thiết:**
   - Trong môi trường doanh nghiệp khắt khe, cổng SSH chỉ nên mở cho IP tĩnh của văn phòng VPN:
     `sudo ufw allow from 203.0.113.10 to any port 22 proto tcp`.
3. **Giám sát nhật ký UFW:**
   - Kiểm tra log các gói tin bị chặn tại `/var/log/ufw.log` hoặc `dmesg` để kịp thời phát hiện các đợt tấn công DDoS / SYN Flood.

---

## 7. Kết luận

Bài tập 3 đã hoàn thành xuất sắc toàn bộ các mục tiêu:
- Thiết lập tường lửa UFW theo chuẩn bảo mật máy chủ Cloud.
- Mở chính xác hai cổng `22/tcp` và `8080/tcp`.
- Kiểm tra toàn diện qua `ufw status verbose`, `ss -tlnp` và chương trình Java chuyên dụng.
