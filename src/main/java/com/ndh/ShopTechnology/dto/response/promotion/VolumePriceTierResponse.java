package com.ndh.ShopTechnology.dto.response.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolumePriceTierResponse {

    private Long id;

    @JsonProperty("product_variant_id")
    private Long productVariantId;

    @JsonProperty("product_id")
    private Long productId;

    private Integer minQuantity;
    private Double unitPrice;
    private Boolean enabled;
}
