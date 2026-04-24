package com.ndh.ShopTechnology.services.order;

import com.ndh.ShopTechnology.dto.response.order.PaymentMethodResponse;

import java.util.List;

public interface PaymentMethodService {

    List<PaymentMethodResponse> listActivePaymentMethods();
}
