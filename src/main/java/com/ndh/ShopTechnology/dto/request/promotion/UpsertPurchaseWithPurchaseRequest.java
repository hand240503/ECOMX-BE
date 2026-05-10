package com.ndh.ShopTechnology.dto.request.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpsertPurchaseWithPurchaseRequest {

    @NotNull
    private Long anchorProductId;

    @NotNull
    private Long companionProductId;

    @NotNull
    @Min(0)
    private Double promoUnitPrice;

    @Min(1)
    private Integer minAnchorQuantity;

    @Min(1)
    private Integer companionPromoUnitsPerAnchor;

    @Min(1)
    private Integer maxCompanionPromoUnits;

    private Boolean enabled;
}
