package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutPricingLineItemResponse {

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("product_variant_id")
    private Long productVariantId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("variant_sku_code")
    private String variantSkuCode;

    @JsonProperty("variant_options")
    private Map<String, String> variantOptions;

    private Integer quantity;
    private String description;

    @JsonProperty("unit_price")
    private Double unitPrice;

    @JsonProperty("line_total")
    private Double lineTotal;

    @JsonProperty("pricing_programs")
    private OrderLinePricingProgramsDto pricingPrograms;
}
