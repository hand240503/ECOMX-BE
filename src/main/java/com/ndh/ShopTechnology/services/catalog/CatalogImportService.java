package com.ndh.ShopTechnology.services.catalog;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Import/upsert thương hiệu và danh mục từ file Excel/CSV/TXT (đúng định dạng file export).
 *
 * <p>Quy tắc upsert: bản ghi đã tồn tại (khớp id, hoặc code) -> CẬP NHẬT (ghi đè);
 * chưa tồn tại -> THÊM MỚI.
 */
public interface CatalogImportService {

    CatalogImportResponse importBrands(MultipartFile file);

    CatalogImportResponse importCategories(MultipartFile file);

    /** Xem trước (dry-run): xác định mỗi dòng sẽ THÊM MỚI / CẬP NHẬT, KHÔNG ghi vào CSDL. */
    CatalogImportResponse previewBrands(MultipartFile file);

    CatalogImportResponse previewCategories(MultipartFile file);
}
