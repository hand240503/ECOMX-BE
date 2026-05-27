package com.ndh.ShopTechnology.dto.response.order;

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
public class OrderLinePricingProgramsDto {

    @JsonProperty("priced_at_epoch_ms")
    private Long pricedAtEpochMillis;

    @JsonProperty("catalog_unit_price")
    private Double catalogUnitPrice;

    @JsonProperty("effective_unit_before_volume_tier")
    private Double effectiveUnitBeforeVolumeTier;

    @JsonProperty("final_unit_price")
    private Double finalUnitPrice;

    @JsonProperty("line_total")
    private Double lineTotal;

    @JsonProperty("price_change")
    private PriceChangeProgramDto priceChange;

    @JsonProperty("volume_tier")
    private VolumeTierProgramDto volumeTier;

    @JsonProperty("purchase_with_purchase")
    private PwpProgramDto purchaseWithPurchase;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PriceChangeProgramDto {
        private Long id;

        @JsonProperty("product_variant_id")
        private Long productVariantId;

        @JsonProperty("base_price")
        private Double basePrice;

        @JsonProperty("sale_price")
        private Double salePrice;

        @JsonProperty("resolved_unit_price")
        private Double resolvedUnitPrice;

        @JsonProperty("start_at_epoch_ms")
        private Long startAtEpochMillis;

        @JsonProperty("end_at_epoch_ms")
        private Long endAtEpochMillis;

        @JsonProperty("quantity_limit")
        private Integer quantityLimit;

        @JsonProperty("sold_quantity")
        private Integer soldQuantity;

        @JsonProperty("required_payment_method_code")
        private String requiredPaymentMethodCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VolumeTierProgramDto {
        private Long id;

        @JsonProperty("min_quantity")
        private Integer minQuantity;

        @JsonProperty("tier_unit_price")
        private Double tierUnitPrice;

        @JsonProperty("aggregate_quantity_for_variant_on_order")
        private Integer aggregateQuantityForVariantOnOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PwpProgramDto {
        @JsonProperty("offer_id")
        private Long offerId;

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

        @JsonProperty("promo_quantity")
        private Integer promoQuantity;

        @JsonProperty("regular_quantity")
        private Integer regularQuantity;

        @JsonProperty("regular_unit_price_after_programs")
        private Double regularUnitPriceAfterPrograms;
    }
}
