package com.ndh.ShopTechnology.dto.request.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderHeaderRequest {

    private String description;

    private Integer typeOrder;

    private String deliveryAddress;

    @DecimalMin(value = "0.0", inclusive = true, message = "deliveryDistanceMeters must be >= 0")
    private Double deliveryDistanceMeters;

    private Long userAddressId;

    /** Kho (store) khách chọn — tính phí ship và trừ tồn tại kho này. Null = dùng kho mặc định. */
    private Long storeId;

    @NotNull(message = "paymentMethodId is required")
    private Long paymentMethodId;

    private String checkoutWorkSessionId;
}
