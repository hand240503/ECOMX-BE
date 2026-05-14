package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một dòng giá catalog cần cập nhật trong luồng {@code PUT} sản phẩm (đã có {@link com.ndh.ShopTechnology.entities.product.PriceEntity#getId()}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProductPriceItemRequest {

    @NotNull(message = "Price id is required")
    private Long id;

    /** Đổi đơn vị tính; bỏ qua hoặc null để giữ unit hiện tại. */
    @JsonProperty("unit_id")
    private Long unitId;

    @NotNull(message = "current_value is required")
    @Min(0)
    @JsonProperty("current_value")
    private Double currentValue;

    @Min(0)
    @JsonProperty("old_value")
    private Double oldValue;
}
