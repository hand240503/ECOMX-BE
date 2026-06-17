# Cấu hình VNPAY: Deploy production nhưng thanh toán dùng Sandbox

> Áp dụng cho dự án deploy lên domain thật `https://ndhtech.id.vn` nhưng **giữ thanh toán ở môi trường Sandbox** (không tiền thật) — phù hợp đồ án/demo.

## 1. Nguyên tắc

| Thành phần | Môi trường | Ghi chú |
|---|---|---|
| Website, API, server | **Production** (domain thật) | Chạy thật trên Internet |
| Cổng thanh toán VNPAY | **Sandbox** | Dùng thẻ test, **không có tiền thật** |

Sandbox vẫn chạy đầy đủ luồng: tạo đơn → chuyển sang trang VNPAY → thanh toán bằng thẻ test → callback về web. Chỉ khác là không phát sinh giao dịch tiền thật. Điều kiện duy nhất: **Return URL / IPN URL phải truy cập được từ Internet** — sau khi deploy lên `ndhtech.id.vn` thì đã thỏa.

> Production thật của VNPAY cần ký hợp đồng doanh nghiệp + mã số thuế → không dùng cho đồ án.

---

## 2. Cấu hình Backend

### 2.1. `application-prod.yml` (hoặc giữ trong `application-local.yml`)

Phần `vnpay` để như sau — `pay-url` trỏ tới **sandbox**:

```yaml
vnpay:
  enabled: true
  tmn-code: ${VNP_TMN_CODE}          # mã merchant sandbox
  hash: ${VNP_HASH_SECRET}           # secret key sandbox
  pay-url: "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"   # 👈 SANDBOX
  return-url: ${VNP_RETURN_URL:https://ndhtech.id.vn/api/v1/payment/vnpay/return}
  frontend-redirect-base: ${VNPAY_FRONTEND_BASE:https://ndhtech.id.vn}
  frontend-result-path: ${VNPAY_FRONTEND_PATH:/payment/vnpay-callback}
  payment-method-code: "VNPAY"
  dev-simulate-success-enabled: false   # 👈 TẮT giả lập, để gọi sandbox thật
```

> Quan trọng: đặt `dev-simulate-success-enabled: false`. Nếu để `true`, hệ thống sẽ **giả lập thành công** mà không thật sự gọi sang VNPAY — không đúng với mục tiêu demo cổng thanh toán.

### 2.2. File biến môi trường trên server `/opt/ecomx/ecomx.env`

Bổ sung các biến VNPAY (điền giá trị sandbox thật của bạn):

```env
VNP_TMN_CODE=ma_tmn_sandbox_cua_ban
VNP_HASH_SECRET=secret_sandbox_cua_ban
VNP_RETURN_URL=https://ndhtech.id.vn/api/v1/payment/vnpay/return
VNPAY_FRONTEND_BASE=https://ndhtech.id.vn
VNPAY_FRONTEND_PATH=/payment/vnpay-callback
```

Sau khi sửa, khởi động lại backend:

```bash
sudo systemctl restart ecomx
```

---

## 3. Cấu hình Frontend (storefront)

Tắt chế độ giả lập thanh toán ở FE (nếu đang bật khi dev). Trong file `ecomx-fe/.env.production`:

```env
VITE_API_V1_BASE_URL=https://ndhtech.id.vn/api/v1
VITE_VNPAY_DEV_SIMULATE_SUCCESS=false
```

Rồi build lại và copy `dist`:

```bash
cd ~/ecomx-fe && npm run build
sudo cp -r dist/* /var/www/storefront/
```

---

## 4. Khai báo trên trang Merchant VNPAY Sandbox

Đăng nhập trang quản trị sandbox: **https://sandbox.vnpayment.vn/merchantv2/** (tài khoản đăng ký tại http://sandbox.vnpayment.vn/devreg/).

Cập nhật 2 URL thành domain thật (thay cho `localhost` lúc dev):

| Mục | Giá trị |
|---|---|
| **Return URL** | `https://ndhtech.id.vn/api/v1/payment/vnpay/return` |
| **IPN URL** | `https://ndhtech.id.vn/api/v1/payment/vnpay/ipn` |

> 2 URL này phải **trùng khít** với cấu hình backend. Sai 1 ký tự là callback/IPN thất bại.

---

## 5. Thẻ test sandbox

Khi tới trang thanh toán VNPAY, chọn ngân hàng **NCB** và nhập thẻ test:

| Trường | Giá trị |
|---|---|
| Số thẻ | `9704198526191432198` |
| Tên chủ thẻ | `NGUYEN VAN A` |
| Ngày phát hành | `07/15` |
| OTP | `123456` |

> Đây là thẻ test chính thức của VNPAY (có thể đổi theo tài liệu mới nhất trên trang sandbox). Không dùng thẻ thật.

---

## 6. Luồng hoạt động

```
User đặt hàng (ndhtech.id.vn)
   │
   ▼
BE tạo URL thanh toán (ký bằng hash secret) ──► chuyển sang sandbox.vnpayment.vn
   │
   ▼
User nhập thẻ test + OTP
   │
   ├──► VNPAY gọi IPN  → https://ndhtech.id.vn/api/v1/payment/vnpay/ipn   (cập nhật trạng thái đơn)
   └──► Redirect Return → https://ndhtech.id.vn/api/v1/payment/vnpay/return
            │
            ▼
        BE redirect về FE: https://ndhtech.id.vn/payment/vnpay-callback (hiện kết quả)
```

---

## 7. Checklist kiểm tra

- [ ] `pay-url` = `https://sandbox.vnpayment.vn/...` (KHÔNG phải domain production VNPAY)
- [ ] `dev-simulate-success-enabled: false`
- [ ] `VITE_VNPAY_DEV_SIMULATE_SUCCESS=false`
- [ ] Return URL & IPN URL trên trang merchant = domain thật, trùng config BE
- [ ] `tmn-code` và `hash` là của tài khoản sandbox
- [ ] Đã `systemctl restart ecomx` và build lại FE
- [ ] Thanh toán thử bằng thẻ test NCB → đơn chuyển trạng thái đúng

---

## 8. Lưu ý

- Sandbox **không phát sinh tiền thật**, hợp lệ để demo và bảo vệ đồ án.
- Nếu bật **OneShield** (proxy iNET) trên DNS, đảm bảo nó không chặn request IPN từ VNPAY tới `/api/v1/payment/vnpay/ipn`. Khi nghi ngờ, tạm tắt OneShield để test cho chắc.
- IPN đến từ server VNPAY (không phải trình duyệt) nên endpoint `/ipn` phải public và không yêu cầu đăng nhập.
- Khi cần chuyển sang production thật sau này: chỉ đổi `pay-url`, `tmn-code`, `hash` sang giá trị production do VNPAY cấp (sau khi ký hợp đồng) — phần code và URL callback giữ nguyên.
