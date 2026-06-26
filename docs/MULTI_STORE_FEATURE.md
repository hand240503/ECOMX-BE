# Tính năng đa kho (multi-store): chọn store → tính ship, quản lý kho, chuyển kho, kiểm tra tồn theo kho

Tài liệu tóm tắt các thay đổi đã thực hiện ở 3 repo: `ecomx-be` (backend), `ecomx-fe admin` (quản trị), `ecomx-fe` (web khách).

## 1. Mô hình dữ liệu (backend)

- **`store`** — kho / cửa hàng: `code` (duy nhất), `name`, `phone`, `addressLine`, `city`, `latitude`, `longitude`, `active`, `isDefault`, `note`. Vừa là điểm xuất hàng (origin tính phí ship), vừa giữ tồn.
- **`store_stock`** — tồn theo từng kho: `(store_id, variant_id)` duy nhất, `on_hand`, `reserved`. Đây là **nguồn sự thật** cho kiểm tra/trừ tồn khi đặt đơn.
- **`inventory_ledger`** — thêm cột `store_id` và 2 loại biến động mới `TRANSFER_OUT`, `TRANSFER_IN`.
- **`orders`** — thêm `store_id` (kho khách chọn cho đơn).
- Tồn aggregate trên `product_variant.on_hand/reserved` được **tự đồng bộ = tổng tồn các kho** sau mỗi biến động (dùng cho hiển thị danh sách/trang sản phẩm).

> Schema tự tạo qua Hibernate `ddl-auto: update`. Theo lựa chọn của bạn, **tồn từng kho bắt đầu = 0**, admin nhập tồn cho từng kho (không migrate tồn cũ).

## 2. Phân quyền

Thêm module **STORE (250xxx)**: `CREATE_STORE 250001`, `READ_STORE 250002`, `UPDATE_STORE 250003`, `DELETE_STORE 250004`.
Module này tự xuất hiện trong catalog phân quyền (trang Chức vụ/Phân quyền của admin).

## 3. API mới / thay đổi (backend)

Quản trị (yêu cầu quyền STORE):
- `GET/POST /admin/stores`, `GET/PUT/DELETE /admin/stores/{id}` — CRUD kho.
- `POST /admin/stores/transfer` — chuyển hàng kho A → kho B (`{fromStoreId, toStoreId, items:[{variantId, quantity}], note}`).
- Tồn theo kho (đổi sang store-scoped):
  - `GET /admin/inventory/stocks?storeId=&q=`
  - `GET /admin/inventory/variants/{variantId}/stores` — tồn 1 biến thể ở mọi kho
  - `GET /admin/inventory/stores/{storeId}/variants/{variantId}`
  - `GET /admin/inventory/stores/{storeId}/variants/{variantId}/ledger`
  - `POST /admin/inventory/import` body `{storeId, variantId, quantity, note}`
  - `POST /admin/inventory/adjust` body `{storeId, variantId, onHand, note}`
  - `POST /admin/inventory/import/excel?storeId=` (multipart `file`)

Công khai (web khách):
- `GET /stores` — danh sách kho đang hoạt động.
- `GET /shipping/stores?address=` — danh sách kho + khoảng cách & **phí ship** tới địa chỉ.
- `GET /shipping/distance-to-store?storeId=&address=`.

Đặt đơn:
- `CreateOrderHeaderRequest` thêm `storeId`. Khi tạo đơn, hệ thống **kiểm tra & trừ tồn tại đúng kho khách chọn**; nếu không gửi `storeId` → dùng **kho mặc định**. Phí ship ưu tiên khoảng cách store→địa chỉ khi có `storeId`.

## 4. Luồng nghiệp vụ tồn kho theo kho

- Đặt đơn → `RESERVE` tại store. COD không đủ tồn → từ chối (CONFLICT). Đơn đã thanh toán (VNPAY) → giữ chỗ best-effort (cho oversell, ghi cảnh báo).
- Hủy/bỏ thanh toán → `RELEASE`. Hoàn thành → `SALE_OUT`. Hoàn tiền → `RETURN_IN`/`RETURN_SCRAP`. Tất cả theo `order.store`.
- Chuyển kho → `TRANSFER_OUT` (kho nguồn, chỉ khi đủ bán được) + `TRANSFER_IN` (kho đích).

## 5. Frontend

- **Admin** (`/admin/warehouse`): chọn kho ở đầu trang; thêm/sửa/xoá kho; nhập/điều chỉnh/import tồn theo kho; **chuyển kho**; xem sổ cái theo kho. Menu/route gated bằng quyền STORE (`canViewStores`).
- **Web khách** (CheckoutPage): khối **"Chọn kho lấy hàng"** gọi `GET /shipping/stores?address=`, hiển thị phí ship từng kho, mặc định chọn kho rẻ nhất; gửi `storeId` + khoảng cách theo store khi đặt đơn.

## 6. Việc cần làm trên máy bạn (chưa chạy được trong môi trường này)

> Môi trường tạo PR không có Maven/JDK17 và đọc file mount bị giới hạn, nên **chưa build/typecheck được**. Vui lòng chạy:

```bash
# Backend (cần JDK 17)
cd ecomx-be && ./mvnw clean compile

# Admin FE
cd "ecomx-fe admin" && npm run build

# Web khách
cd ecomx-fe && npm run build
```

Sau khi chạy backend lần đầu (Hibernate tạo bảng), hãy:
1. Vào trang **Phân quyền**, cấp quyền STORE cho role phù hợp.
2. Vào **Kho → Quản lý kho**, tạo ít nhất 1 kho (đặt `isDefault`, điền `latitude/longitude` để tính ship).
3. Nhập tồn cho từng kho (tồn bắt đầu = 0).
