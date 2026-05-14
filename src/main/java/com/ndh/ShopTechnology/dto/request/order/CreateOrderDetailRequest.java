package com.ndh.ShopTechnology.dto.request.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một dòng trên bảng {@code order_detail}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderDetailRequest {

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    /**
     * Ưu tiên khi đặt hàng — id biến thể (SKU) có giá riêng.
     */
    private Long productVariantId;

    /**
     * Legacy: id SPU — server sẽ chọn biến thể active đầu tiên nếu {@link #productVariantId} null.
     */
    private Long productId;

    private String description;
}
