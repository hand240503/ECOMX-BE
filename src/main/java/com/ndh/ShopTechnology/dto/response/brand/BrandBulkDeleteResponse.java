package com.ndh.ShopTechnology.dto.response.brand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả xóa thương hiệu hàng loạt.
 *
 * <p>Sản phẩm thuộc thương hiệu bị xóa được gỡ thương hiệu (brand = null).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandBulkDeleteResponse {

    /** Số thương hiệu yêu cầu xóa. */
    private int requested;

    /** Số thương hiệu đã xóa thành công. */
    private int deleted;

    /** Số sản phẩm đã được gỡ thương hiệu (set null). */
    private int productsDetached;

    /** ID không tìm thấy / bỏ qua. */
    @Builder.Default
    private List<Long> notFoundIds = new ArrayList<>();
}
