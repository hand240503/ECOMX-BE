package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.response.product.ProductImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Import sản phẩm hàng loạt từ file Excel (.xlsx) hoặc CSV/TXT.
 *
 * <p>Mỗi sản phẩm có thể trải trên nhiều dòng: dòng đầu (có tên sản phẩm) khai báo
 * thông tin chung + biến thể thứ nhất; các dòng sau bỏ trống tên sản phẩm sẽ được
 * gom làm biến thể bổ sung của cùng sản phẩm đó.
 */
public interface ProductImportService {

    /**
     * Đọc file, tạo sản phẩm + biến thể + giá + tồn kho.
     * Mỗi sản phẩm được lưu trong một transaction riêng: dòng hợp lệ vẫn tạo,
     * dòng lỗi được báo cáo lại (không chặn cả file).
     */
    ProductImportResponse importProducts(MultipartFile file);

    /**
     * Xem trước (dry-run): phân tích file, KHÔNG ghi CSDL. Mỗi sản phẩm trả về hành động
     * mặc định: CẬP NHẬT nếu đã tồn tại (khớp SKU hoặc tên), ngược lại THÊM MỚI — kèm khóa
     * định danh để người dùng chỉnh chọn trước khi xác nhận.
     */
    ProductImportResponse previewProducts(MultipartFile file);

    /**
     * Import có lựa chọn hành động theo từng sản phẩm.
     *
     * @param actions map khóa sản phẩm -> "CREATE" | "UPDATE" (lấy từ bước xem trước).
     *                Khóa không có trong map sẽ dùng mặc định (UPDATE nếu đã tồn tại, ngược lại CREATE).
     */
    ProductImportResponse importProducts(MultipartFile file, Map<String, String> actions);

    /** Sinh file Excel mẫu (kèm tiêu đề cột + 1 ví dụ) để người dùng tải về điền. */
    byte[] buildTemplateXlsx();
}
