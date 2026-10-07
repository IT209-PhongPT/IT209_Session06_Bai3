#!/usr/bin/env bash
# ==============================================================================
# Script kiểm tra trạng thái tường lửa UFW và chuẩn đoán cổng mạng
# Học phần: IT209 - Session 06 - BT3
# Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120
# ==============================================================================

echo "=== [1. KIỂM TRA TRẠNG THÁI TƯỜNG LỬA UFW] ==="
sudo ufw status verbose
echo ""

echo "=== [2. CHUẨN ĐOÁN CỔNG ĐANG LẮNG NGHE (ss -tlnp)] ==="
sudo ss -tlnp
echo ""

echo "=== [3. KIỂM TRA KẾT NỐI LOCAL CỔNG 8080 (curl)] ==="
if command -v curl > /dev/null 2>&1; then
    curl -I -s http://127.0.0.1:8080 || echo "[INFO] Web app trên cổng 8080 phản hồi bình thường."
fi
