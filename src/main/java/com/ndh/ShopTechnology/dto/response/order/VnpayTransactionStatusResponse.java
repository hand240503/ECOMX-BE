package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VnpayTransactionStatusResponse {

    String transactionPublicId;

    String vnpTransactionStatus;

    String vnpTransactionStatusMessage;

    String internalState;

    String vnpayTxnRef;

    Long orderId;
}
