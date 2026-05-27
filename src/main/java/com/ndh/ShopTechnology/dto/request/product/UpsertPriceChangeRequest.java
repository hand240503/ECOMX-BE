package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpsertPriceChangeRequest {

    @NotNull
    @Min(0)
    private Double basePrice;

    @Min(0)
    private Double salePrice;

    @NotNull
    private Date startAt;

    private Date endAt;

    private Boolean enabled;

    @Min(value = 1, message = "quantityLimit phải >= 1 nếu đặt")
    private Integer quantityLimit;

    @Min(value = 1, message = "maxPerCustomer phải >= 1 nếu đặt")
    private Integer maxPerCustomer;

    @Size(max = 64)
    private String requiredPaymentMethodCode;
}
