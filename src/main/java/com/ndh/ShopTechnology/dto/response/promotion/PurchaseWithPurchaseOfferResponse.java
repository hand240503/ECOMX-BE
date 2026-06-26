package com.ndh.ShopTechnology.dto.response.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseWithPurchaseOfferResponse {

    private Long id;
    private Long anchorProductId;
    private Long companionProductId;
    private Long anchorVariantId;
    private Long companionVariantId;
    private Double promoUnitPrice;
    private Integer minAnchorQuantity;
    private Integer companionPromoUnitsPerAnchor;
    private Integer maxCompanionPromoUnits;
    private Boolean enabled;

    /** Khung thời gian áp dụng (null = không giới hạn). */
    private Date startAt;
    private Date endAt;
}
