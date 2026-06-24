package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import org.springframework.web.multipart.MultipartFile;

/** Import / upsert nhân viên nội bộ từ file Excel/CSV/TXT. */
public interface StaffImportService {

    /**
     * Đọc file và upsert từng nhân viên. Khóa định danh: username → email → telephone.
     * Không xóa nhân viên vắng mặt, không ghi đè mật khẩu khi cập nhật.
     */
    CatalogImportResponse importStaff(MultipartFile file);

    /** Tạo file Excel mẫu để tải về. */
    byte[] buildTemplateXlsx();
}
