package com.ndh.ShopTechnology.dto.response.product;

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
public class ProductPurchaseWithPurchaseProgramResponse {

    @JsonProperty("role")
    private String role;

    private Long id;

    @JsonProperty("anchor_product_id")
    private Long anchorProductId;

    @JsonProperty("companion_product_id")
    private Long companionProductId;

    @JsonProperty("anchor_variant_id")
    private Long anchorVariantId;

    @JsonProperty("companion_variant_id")
    private Long companionVariantId;

    @JsonProperty("promo_unit_price")
    private Double promoUnitPrice;

    @JsonProperty("min_anchor_quantity")
    private Integer minAnchorQuantity;

    @JsonProperty("companion_promo_units_per_anchor")
    private Integer companionPromoUnitsPerAnchor;

    @JsonProperty("max_companion_promo_units")
    private Integer maxCompanionPromoUnits;

    @JsonProperty("anchor_product_name")
    private String anchorProductName;

    @JsonProperty("companion_product_name")
    private String companionProductName;

    @JsonProperty("anchor_product_main_image_url")
    private String anchorProductMainImageUrl;

    @JsonProperty("companion_product_main_image_url")
    private String companionProductMainImageUrl;

    private Boolean enabled;
}
