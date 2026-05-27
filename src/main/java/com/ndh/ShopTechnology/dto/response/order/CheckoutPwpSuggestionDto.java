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
public class CheckoutPwpSuggestionDto {

    @JsonProperty("offer_id")
    private Long offerId;

    @JsonProperty("anchor_product_id")
    private Long anchorProductId;

    @JsonProperty("anchor_variant_id")
    private Long anchorVariantId;

    @JsonProperty("companion_product_id")
    private Long companionProductId;

    @JsonProperty("companion_variant_id")
    private Long companionVariantId;

    @JsonProperty("companion_product_name")
    private String companionProductName;

    @JsonProperty("companion_variant_sku_code")
    private String companionVariantSkuCode;

    @JsonProperty("companion_variant_options")
    private Map<String, String> companionVariantOptions;

    @JsonProperty("companion_thumbnail_url")
    private String companionThumbnailUrl;

    @JsonProperty("promo_unit_price")
    private Double promoUnitPrice;

    @JsonProperty("companion_regular_price")
    private Double companionRegularPrice;

    @JsonProperty("min_anchor_quantity")
    private Integer minAnchorQuantity;

    @JsonProperty("companion_promo_units_per_anchor")
    private Integer companionPromoUnitsPerAnchor;

    @JsonProperty("max_companion_promo_units")
    private Integer maxCompanionPromoUnits;
}
