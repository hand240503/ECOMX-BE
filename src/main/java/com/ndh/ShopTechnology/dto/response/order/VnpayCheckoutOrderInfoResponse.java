package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VnpayCheckoutOrderInfoResponse {

    private Long orderId;
    private String orderCode;
    private String description;
    private Integer typeOrder;
    private String deliveryAddress;
    private Double total;
    private Double deliveryDistanceMeters;
    private Long shippingFeeVnd;
    private List<OrderDetailResponse> orderDetails;
}
