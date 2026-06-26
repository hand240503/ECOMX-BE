package com.ndh.ShopTechnology.dto.request.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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
    private Long anchorVariantId;

    @NotNull
    private Long companionVariantId;

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

    /** Khung thời gian áp dụng (null = không giới hạn). Với import: chọn sau khi review. */
    private Date startAt;
    private Date endAt;
}
