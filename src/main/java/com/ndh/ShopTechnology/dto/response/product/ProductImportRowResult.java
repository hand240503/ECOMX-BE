package com.ndh.ShopTechnology.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả import của một sản phẩm (một nhóm dòng) trong file tải lên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportRowResult {

    /** Dòng đầu tiên của sản phẩm trong file (1-based, tính cả dòng tiêu đề). */
    private Integer rowNumber;

    /** Tên sản phẩm đọc được từ file. */
    private String productName;

    /** Khóa định danh ổn định để FE gửi lại lựa chọn (CREATE/UPDATE) khi xác nhận. */
    private String key;

    /** Hành động: CREATE = thêm mới, UPDATE = cập nhật (mặc định theo việc SP đã tồn tại hay chưa). */
    private String action;

    /** true = sản phẩm đã tồn tại trong CSDL (khớp theo SKU hoặc tên). */
    private boolean exists;

    /** true = tạo/cập nhật thành công. */
    private boolean success;

    /** ID sản phẩm vừa tạo/cập nhật (có khi success = true, hoặc id SP đã tồn tại ở bước xem trước). */
    private Long productId;

    /** Số biến thể đã tạo cho sản phẩm này. */
    private Integer variantCount;

    /** Thông báo: lý do lỗi khi thất bại, hoặc mô tả khi thành công. */
    private String message;
}
