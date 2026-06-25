package com.ndh.ShopTechnology.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả import của một biến thể (một dòng) trong file tải lên ở trang chi tiết sản phẩm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantImportRowResult {

    /** Dòng trong file (1-based, tính cả dòng tiêu đề). */
    private Integer rowNumber;

    /** Mã SKU biến thể đọc được từ file. */
    private String skuCode;

    /** Mô tả thuộc tính (option_values) đọc được, dạng "Khóa=Giá trị; ...". */
    private String optionsLabel;

    /** Khóa định danh ổn định để FE gửi lại lựa chọn (CREATE/UPDATE) khi xác nhận. */
    private String key;

    /** Hành động: CREATE = thêm mới, UPDATE = cập nhật biến thể đã có. */
    private String action;

    /** true = biến thể đã tồn tại trong sản phẩm này. */
    private boolean exists;

    /** true = xử lý thành công. */
    private boolean success;

    /** ID biến thể vừa tạo/cập nhật (hoặc id biến thể đã tồn tại ở bước xem trước). */
    private Long variantId;

    /** Thông báo: lý do lỗi khi thất bại, hoặc mô tả khi thành công. */
    private String message;
}
