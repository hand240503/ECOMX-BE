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
 * Body cho create / update giá catalog hằng ngày (PriceEntity) trên 1 sản phẩm đã tồn tại.
 *
 * <p>Khác với {@link CreatePriceRequest}: ở create-product flow, {@code unitId} bắt buộc kèm sản phẩm.
 * Ở admin-price flow, {@code unitId} bắt buộc khi POST, có thể null khi PUT (giữ nguyên unit cũ).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpsertProductPriceRequest {

    /**
     * Biến thể nhận giá mới. Với POST: bắt buộc nếu sản phẩm có nhiều biến thể;
     * có thể bỏ qua khi chỉ có một biến thể active (server tự chọn).
     */
    @JsonProperty("product_variant_id")
    private Long productVariantId;

    /**
     * Đơn vị tính (cùng bảng {@code units}).
     * - POST: bắt buộc.
     * - PUT: có thể null (giữ nguyên).
     */
    @JsonProperty("unit_id")
    private Long unitId;

    /** Giá hiện tại (VND) — luôn bắt buộc. */
    @NotNull
    @Min(0)
    @JsonProperty("current_value")
    private Double currentValue;

    /** Giá cũ (gạch ngang) — không bắt buộc; null sẽ map về 0. */
    @Min(0)
    @JsonProperty("old_value")
    private Double oldValue;
}
