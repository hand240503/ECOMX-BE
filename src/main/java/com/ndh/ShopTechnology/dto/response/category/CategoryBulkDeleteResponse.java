package com.ndh.ShopTechnology.dto.response.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả xóa danh mục hàng loạt.
 *
 * <p>Sản phẩm thuộc danh mục bị xóa được gỡ danh mục (category = null);
 * danh mục con được đưa lên gốc (parent = null).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBulkDeleteResponse {

    /** Số danh mục yêu cầu xóa. */
    private int requested;

    /** Số danh mục đã xóa thành công. */
    private int deleted;

    /** Số sản phẩm đã được gỡ danh mục (set null). */
    private int productsDetached;

    /** Số danh mục con đã được đưa lên gốc (parent set null). */
    private int childrenDetached;

    /** ID không tìm thấy / bỏ qua. */
    @Builder.Default
    private List<Long> notFoundIds = new ArrayList<>();
}
