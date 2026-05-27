package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VnpayPendingTransactionResponse {

    String transactionPublicId;

    String state;

    Date expiresAt;
    Double total;
    PaymentMethodSummaryResponse paymentMethod;

    String vnpayTxnRef;

    VnpayCheckoutOrderInfoResponse orderInfo;

    OrderResponse order;

    String message;
}
