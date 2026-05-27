package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderResultResponse {

    String outcome;
    OrderResponse order;
    Long checkoutSessionId;
    String transactionPublicId;
    Double pendingTotal;
    Double deliveryDistanceMeters;
    Long shippingFeeVnd;
    PaymentMethodSummaryResponse paymentMethod;
    String message;
}
