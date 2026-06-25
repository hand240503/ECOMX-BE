package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Gán DANH MỤC / THƯƠNG HIỆU cho sản phẩm hàng loạt từ file Excel/CSV.
 *
 * <p>File CHỈ chứa: sku (xác định sản phẩm) + brand_code và/hoặc category_code (KHÔNG có id).
 * Chức năng này CHỈ cập nhật liên kết product → brand/category; KHÔNG tạo/sửa bản ghi
 * thương hiệu/danh mục và KHÔNG đụng thông tin khác của sản phẩm. Cột để trống = giữ nguyên.
 */
public interface ProductCatalogAssignService {

    /** Đọc file và gán brand/category (theo code) cho từng sản phẩm (theo sku). */
    CatalogImportResponse importAssign(MultipartFile file);

    /** File Excel mẫu: cột sku + brand_code + category_code. */
    byte[] buildTemplateXlsx();
}
