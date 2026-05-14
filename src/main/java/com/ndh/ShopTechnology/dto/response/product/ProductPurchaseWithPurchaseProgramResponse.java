package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chương trình PWP đang bật liên quan đến một SPU: vai trò của SPU hiện tại trên API là neo hay đi kèm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductPurchaseWithPurchaseProgramResponse {

    /** {@code companion} — SP này được giá {@code promo_unit_price} khi có neo; {@code anchor} — SP này là neo cho các offer đi kèm. */
    @JsonProperty("role")
    private String role;

    private Long id;

    @JsonProperty("anchor_product_id")
    private Long anchorProductId;

    @JsonProperty("companion_product_id")
    private Long companionProductId;

    @JsonProperty("promo_unit_price")
    private Double promoUnitPrice;

    @JsonProperty("min_anchor_quantity")
    private Integer minAnchorQuantity;

    @JsonProperty("companion_promo_units_per_anchor")
    private Integer companionPromoUnitsPerAnchor;

    @JsonProperty("max_companion_promo_units")
    private Integer maxCompanionPromoUnits;

    private Boolean enabled;
}
