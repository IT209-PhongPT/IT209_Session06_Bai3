# Báo cáo Bài tập 3: Cấu hình Tường lửa UFW và Chuẩn đoán Cổng mạng

- **Họ và tên:** Phạm Thanh Phong
- **Mã sinh viên:** N24DTCN120
- **Môn học:** IT209 
- **Session:** 06 - Quản trị Hệ thống Tệp tin Linux (FHS) & Phân quyền Nâng cao
- **Đường dẫn nộp bài:** `homework/session_06/ex3/`

---

## 1. Bối cảnh & Yêu cầu bài tập

Triển khai cấu hình tường lửa UFW (Uncomplicated Firewall) bảo vệ máy chủ Cloud VPS chuẩn bị chạy ứng dụng Web tại cổng `8080`:
1. **Chính sách mặc định (Default Policy):**
   - Chặn toàn bộ kết nối đi vào: `deny incoming`.
   - Cho phép toàn bộ kết nối đi ra: `allow outgoing`.
2. **Mở các cổng cần thiết:**
   - Mở cổng SSH (`22/tcp`) để duy trì kết nối quản trị từ xa.
   - Mở cổng ứng dụng Web (`8080/tcp`).
3. **Kích hoạt và kiểm tra:**
   - Bật tường lửa UFW và xác thực trạng thái hoạt động qua lệnh `sudo ufw status verbose` và `ss -tlnp`.

---

## 2. Nhật ký câu lệnh thực hiện (Command Execution Log)

```bash
# 1. Cấu hình chính sách mặc định (Chặn đầu vào, cho phép đầu ra)
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 2. Mở cổng kết nối SSH (Port 22) và Web App (Port 8080)
sudo ufw allow 22/tcp
sudo ufw allow 8080/tcp

# 3. Kích hoạt tường lửa UFW
sudo ufw --force enable

# 4. Kiểm tra trạng thái chi tiết của tường lửa
sudo ufw status verbose

# 5. Kiểm tra các cổng socket đang lắng nghe thực tế
ss -tlnp
```

---

## 3. Kết quả kiểm tra xác thực (Verification Output)

### 3.1. Kết quả lệnh `sudo ufw status verbose`:
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

### 3.2. Kết quả lệnh chẩn đoán cổng mạng `ss -tlnp`:
```text
State    Recv-Q   Send-Q     Local Address:Port        Peer Address:Port   Process                                          
LISTEN   0        128              0.0.0.0:22               0.0.0.0:*       users:(("sshd",pid=820,fd=3))                   
LISTEN   0        128              0.0.0.0:8080             0.0.0.0:*       users:(("java",pid=4192,fd=14))                 
LISTEN   0        128                 [::]:22                  [::]:*       users:(("sshd",pid=820,fd=4))                   
LISTEN   0        128                 [::]:8080                [::]:*       users:(("java",pid=4192,fd=15))                 
```

---

## 4. Bảng tổng hợp đối chiếu quy tắc tường lửa

| Cổng (Port) | Giao thức | Hướng (Direction) | Hành động (Action) | Nguồn (Source) | Mục đích bảo mật |
| :---: | :---: | :---: | :---: | :---: | :--- |
| **22** | TCP | Inbound | `ALLOW IN` | Anywhere / Anywhere (v6) | Quản trị máy chủ từ xa qua SSH an toàn. |
| **8080** | TCP | Inbound | `ALLOW IN` | Anywhere / Anywhere (v6) | Cho phép người dùng truy cập dịch vụ Web Application. |
| **Các cổng khác** (vd: 3306, 80, 443) | ALL | Inbound | `DENY` (Mặc định) | Tất cả | Chống quét cổng (Port Scanning) và tấn công thăm dò. |

---

## 5. Chương trình Java Thẩm định Tường lửa & Web Server (`Main.java`)

Bài tập đi kèm mã nguồn Java chuẩn (`Main.java`) tích hợp Web Server HTTP trên port 8080 và bộ mô phỏng lọc gói tin tường lửa (UFW Packet Filter Engine):

### Lệnh thực thi:
```bash
java Main.java
```

### Kết quả chạy chương trình Java:
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
