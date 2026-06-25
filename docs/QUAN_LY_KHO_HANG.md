# Quản lý kho hàng & đồng bộ tồn kho

Tài liệu mô tả chức năng quản lý kho và cách tồn kho được đồng bộ với mọi luồng
đơn hàng (bán ra và hoàn hàng).

## Mô hình

Tồn kho quản lý ở cấp **biến thể (SKU / `ProductVariant`)** với 2 con số:

- `on_hand`  — tồn thực tế trong kho.
- `reserved` — đang giữ cho các đơn chưa hoàn thành.
- `available = on_hand − reserved` (tính toán, không lưu).

Mọi biến động được ghi vào **sổ cái** `inventory_ledger` (`InventoryLedgerEntity`):
loại biến động, số lượng delta, số dư `on_hand` trước/sau (`sum_begin`/`sum_end`),
dòng đơn nguồn (nếu có) và ghi chú — phục vụ truy vết & đối soát.

Chống bán âm bằng **UPDATE có điều kiện (atomic)** ở tầng DB, nên 2 đơn mua đồng
thời món cuối không thể vượt tồn.

## Bản đồ case → trạng thái kho

| Sự kiện | on_hand | reserved | Bút toán ledger |
|---|---|---|---|
| Đặt COD | — | +qty | RESERVE |
| VNPAY thanh toán OK (tạo đơn) | — | +qty | RESERVE |
| Giao xong → COMPLETED | −qty | −qty | SALE_OUT |
| Khách hủy (chờ xác nhận) | — | −qty | RELEASE |
| Admin hủy (trước hoàn thành) | — | −qty | RELEASE |
| Hoàn tiền REFUNDED, hàng tốt | +qty | — | RETURN_IN |
| Hoàn tiền REFUNDED, hàng lỗi | — | — | RETURN_SCRAP |
| Nhập kho thủ công | +qty | — | IMPORT |
| Điều chỉnh / kiểm kê | =mới | — | ADJUST |

`soldCount` của sản phẩm: +qty khi COMPLETED, −qty khi REFUNDED.

## Tính idempotent

Mọi thao tác theo đơn an toàn khi gọi lặp (retry VNPAY IPN, event trùng,
double-click): kiểm tra sự tồn tại bút toán tương ứng trong ledger trước khi áp
dụng (ví dụ đã có `SALE_OUT` cho dòng đơn thì không xuất lần nữa).

## Điểm móc trong code

- `InventoryService` / `InventoryServiceImpl` — toàn bộ logic kho.
- `OrderServiceImpl`:
  - `persistOrderFromDraft` → `reserveForOrder` (giữ hàng khi tạo đơn).
  - `cancelMyOrder` / `adminUpdateOrderStatus(CANCELLED)` → `releaseForOrder`.
  - `adminUpdateOrderStatus(COMPLETED)` → `commitSaleForOrder` + cộng soldCount.
  - `adminUpdateReturnStatus(REFUNDED)` → `restockForOrder` + trừ soldCount.

## API admin

`{api.prefix}/admin/inventory`

- `GET  /variants/{variantId}`         — xem tồn hiện tại.
- `GET  /variants/{variantId}/ledger`  — lịch sử biến động.
- `POST /import`  body `{ variantId, quantity, note }` — nhập kho.
- `POST /adjust`  body `{ variantId, onHand, note }`    — điều chỉnh/kiểm kê.

Cập nhật trả hàng (`PATCH /admin/orders/{id}/return-status`) nhận thêm cờ
`restockToSellable` (mặc định `true`): `false` = hàng lỗi, không cộng tồn bán được.

## Lưu ý vận hành

- Sản phẩm hiện có `on_hand = 0` cho tới khi nhập kho — cần nhập tồn ban đầu.
- Đơn cũ tạo trước khi bật tính năng (không có bút toán RESERVE) sẽ **không** bị
  trừ kho khi hoàn thành (tránh âm kho); chỉ áp dụng cho đơn mới.
- `ddl-auto: update` sẽ tự tạo cột `on_hand`, `reserved` và bảng `inventory_ledger`.
