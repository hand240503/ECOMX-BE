# Import bằng Excel: Tồn kho & các chương trình KM (PC / PWP / Mix & Match)

Bổ sung các chức năng nhập dữ liệu hàng loạt bằng file Excel/CSV/TXT cho ADMIN,
tách bạch theo từng nghiệp vụ:

- **Import sản phẩm** chỉ tạo *product + biến thể + giá* (không còn cột `on_hand`).
- **Import tồn kho** (Quản lý kho) mới là nơi nhập *số lượng* cho biến thể.
- **Import chương trình KM**: PC (đổi giá), PWP (mua kèm), Mix & Match (giá theo số lượng).

Mỗi dòng xử lý độc lập trong transaction riêng (gọi lại service nghiệp vụ sẵn có nên
ghi đủ sổ cái / lịch sử); dòng lỗi được báo cáo, không chặn cả file. Kết quả trả về theo
`CatalogImportResponse` (totalRows, createdCount, updatedCount, failureCount, results[]).

Biến thể tra theo `variant_id` **hoặc** `sku_code` (ưu tiên id nếu điền cả hai).

## 1. Import tồn kho

Cột: `variant_id`, `sku_code`, `quantity`, `mode`, `note`.

- `mode = add` (mặc định): CỘNG THÊM vào tồn hiện có — ghi ledger `IMPORT` (quantity > 0).
- `mode = set`: ĐẶT TUYỆT ĐỐI tồn về đúng số — ghi ledger `ADJUST` (kiểm kê, quantity >= 0).

API (quyền `100003` UPDATE_PRODUCT):

- `POST /api/v1/admin/inventory/import/excel` — multipart `file`.
- `GET  /api/v1/admin/inventory/import/template` — tải file mẫu.

UI: trang **Quản lý kho** → nút "Nhập kho từ Excel".

## 2. Import chương trình đổi giá (PC — Price Change)

Cột: `variant_id`, `sku_code`, `base_price` (bắt buộc), `sale_price`, `start_at` (bắt buộc),
`end_at`, `enabled`, `quantity_limit`, `max_per_customer`, `required_payment_method_code`.

Ngày nhận `yyyy-MM-dd HH:mm`, `yyyy-MM-dd`, `dd/MM/yyyy`…

## 3. Import mua kèm (PWP — Purchase With Purchase)

Cột: `anchor_variant_id`/`anchor_sku_code`, `companion_variant_id`/`companion_sku_code`,
`promo_unit_price` (bắt buộc), `min_anchor_quantity`, `companion_promo_units_per_anchor`,
`max_companion_promo_units`, `enabled`. Mỗi biến thể kèm chỉ thuộc tối đa một chương trình PWP.

## 4. Import giá theo số lượng (Mix & Match — Volume Price Tier)

Cột: `variant_id`, `sku_code`, `min_quantity`, `unit_price`, `enabled`.

Một biến thể có thể có nhiều dòng (nhiều mốc). Import **gộp** với mốc đang có (thêm/cập nhật;
mốc cùng `min_quantity` bị ghi đè), KHÔNG xóa mốc cũ — để xóa hãy dùng giao diện Mix & Match.

API các chương trình KM (quyền `100001` CREATE_PRODUCT), prefix `/api/v1/admin/promotions`:

| Chương trình | Import | Template |
|---|---|---|
| PC  | `POST /price-changes/import` | `GET /price-changes/import/template` |
| PWP | `POST /purchase-with-purchase/import` | `GET /purchase-with-purchase/import/template` |
| Mix & Match | `POST /volume-price-tiers/import` | `GET /volume-price-tiers/import/template` |

UI: các trang **Price change**, **Mua kèm (PwP)**, **Mix & match** → nút "Nhập Excel".

## Các file đã thêm / sửa

Backend (ecomx-be):
- `services/importexport/ImportSupport.java` — tiện ích đọc file, ánh xạ tiêu đề, parse số/ngày/bool, tra biến thể, sinh file mẫu.
- `services/inventory/InventoryImportService.java` (+ `impl/InventoryImportServiceImpl.java`).
- `services/promotion/PromotionImportService.java` (+ `impl/PromotionImportServiceImpl.java`).
- `controller/admin/AdminPromotionImportController.java`; cập nhật `AdminInventoryController.java`.
- `repository/ProductVariantRepository.java` — finder theo `sku_code`.
- `services/product/impl/ProductImportServiceImpl.java` — bỏ cột `on_hand`.

Frontend admin (ecomx-fe admin):
- `api/config/apiEndpoints.ts` — endpoint import tồn kho + 3 chương trình KM.
- `api/services/importHelpers.ts` — post file + tải template dùng chung.
- `api/services/adminInventoryService.ts`, `adminPromotionService.ts` — method import/template.
- `admin/components/AdminBulkImportModal.tsx` — modal import dùng chung (kéo-thả + bảng kết quả).
- Cập nhật `AdminWarehousePage`, `AdminPriceChangesPage`, `AdminPurchaseWithPurchasePage`, `AdminMixAndMatchPage` — nút "Nhập Excel" + modal.
