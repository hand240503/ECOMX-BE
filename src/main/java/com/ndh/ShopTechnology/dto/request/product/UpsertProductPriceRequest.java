package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpsertProductPriceRequest {

    @JsonProperty("product_variant_id")
    private Long productVariantId;

    @JsonProperty("unit_id")
    private Long unitId;

    @NotNull
    @Min(0)
    @JsonProperty("current_value")
    private Double currentValue;

    @Min(0)
    @JsonProperty("old_value")
    private Double oldValue;

    /** Tên hiển thị tuỳ chỉnh (ví dụ: "Hộp 6 chiếc"). Không bắt buộc. */
    @JsonProperty("display_name")
    private String displayName;
}
