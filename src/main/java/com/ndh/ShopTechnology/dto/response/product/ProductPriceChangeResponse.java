package com.ndh.ShopTechnology.dto.response.product;

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
public class ProductPriceChangeResponse {

    private Long id;
    private Long productId;
    private Long productVariantId;
    private Double basePrice;
    private Double salePrice;
    private Date startAt;
    private Date endAt;
    private Boolean enabled;

    private Integer quantityLimit;

    private Integer soldQuantity;

    private Integer remainingQuantity;

    private Integer maxPerCustomer;

    private String requiredPaymentMethodCode;
}
