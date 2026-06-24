package com.ndh.ShopTechnology.services.unit;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import org.springframework.web.multipart.MultipartFile;

/** Import / upsert đơn vị tính từ file Excel/CSV/TXT. */
public interface UnitImportService {

    /** Đọc file và upsert từng đơn vị tính. Mỗi dòng xử lý độc lập. */
    CatalogImportResponse importUnits(MultipartFile file);

    /** Tạo file Excel mẫu để tải về. */
    byte[] buildTemplateXlsx();
}
