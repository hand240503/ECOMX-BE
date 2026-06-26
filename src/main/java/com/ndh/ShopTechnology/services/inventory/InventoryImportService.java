package com.ndh.ShopTechnology.services.inventory;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Import tồn kho hàng loạt từ file Excel/CSV/TXT.
 *
 * <p>Mỗi dòng = một biến thể (tra theo variant_id hoặc sku_code) + số lượng. Cột "mode"
 * quyết định: CỘNG THÊM (nhập kho, ghi ledger IMPORT) hoặc ĐẶT TUYỆT ĐỐI (kiểm kê, ghi ADJUST).
 * Mỗi dòng xử lý độc lập: dòng lỗi được báo cáo, không chặn cả file.
 */
public interface InventoryImportService {

    /** Import tồn kho vào một kho (store) cụ thể. */
    CatalogImportResponse importStock(Long storeId, MultipartFile file);

    /** Sinh file Excel mẫu để người dùng tải về điền. */
    byte[] buildTemplateXlsx();
}
