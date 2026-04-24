package com.ndh.ShopTechnology.dto.response.payment;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VnpayCreatePaymentData {
    String paymentUrl;
    String txnRef;
    long vnpAmount;
}
