#!/usr/bin/env bash
# ==============================================================================
# Học phần: IT209 - Phát triển ứng dụng Web / Cloud Infrastructure
# Session: 06 - Bài tập 3: Cấu hình tường lửa UFW và chuẩn đoán cổng mạng
# Sinh viên thực hiện: Phạm Thanh Phong - MSSV: N24DTCN120
# ==============================================================================

set -euo pipefail

echo "======================================================================"
echo " [IT209 - Session 06 - BT3] THIẾT LẬP TƯỜNG LỬA UFW CHO MÁY CHỦ CLOUD "
echo "======================================================================"

# 1. Cấu hình chính sách mặc định (Default Policy)
echo "[1/4] Thiết lập chính sách mặc định: Deny Incoming / Allow Outgoing..."
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 2. Mở các cổng mạng cần thiết
echo "[2/4] Mở cổng SSH (22/tcp) và Web Application (8080/tcp)..."
sudo ufw allow 22/tcp comment 'SSH Remote Management'
sudo ufw allow 8080/tcp comment 'Web Application Service'

# 3. Kích hoạt tường lửa UFW
echo "[3/4] Kích hoạt tường lửa UFW..."
sudo ufw --force enable

# 4. Kiểm tra trạng thái chi tiết
echo "----------------------------------------------------------------------"
echo "[4/4] TRẠNG THÁI TƯỜNG LỬA (sudo ufw status verbose):"
echo "----------------------------------------------------------------------"
sudo ufw status verbose

echo "======================================================================"
echo "[SUCCESS] Thiết lập tường lửa UFW hoàn tất thành công!"
echo "======================================================================"
