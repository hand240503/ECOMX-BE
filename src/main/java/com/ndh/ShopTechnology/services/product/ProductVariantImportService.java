package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.response.product.VariantImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Import biến thể (phân loại) cho MỘT sản phẩm từ file Excel (.xlsx) / CSV / TXT.
 *
 * <p>Dùng ở trang CHI TIẾT sản phẩm: chọn sản phẩm trước, sau đó tải file biến thể
 * (cột sku_code, option_values, sort_order, active) để nạp vào sản phẩm đó.
 * Mỗi DÒNG = một biến thể. KHÔNG đụng tới giá, tồn kho, danh mục, thương hiệu.
 */
public interface ProductVariantImportService {

    /**
     * Xem trước (dry-run): phân tích file, KHÔNG ghi CSDL. Mỗi biến thể trả về hành động
     * mặc định: CẬP NHẬT nếu đã có trong sản phẩm (khớp sku_code hoặc bộ option),
     * ngược lại THÊM MỚI — kèm khóa định danh để người dùng chỉnh chọn trước khi xác nhận.
     */
    VariantImportResponse previewVariants(Long productId, MultipartFile file);

    /**
     * Import có lựa chọn hành động theo từng biến thể.
     *
     * @param actions map khóa biến thể -> "CREATE" | "UPDATE" (lấy từ bước xem trước).
     *                Khóa không có trong map sẽ dùng mặc định.
     */
    VariantImportResponse importVariants(Long productId, MultipartFile file, Map<String, String> actions);

    /** Sinh file Excel mẫu (kèm tiêu đề cột + ví dụ) cho import biến thể. */
    byte[] buildTemplateXlsx();
}
