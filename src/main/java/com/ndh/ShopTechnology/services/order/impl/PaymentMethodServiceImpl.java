package com.ndh.ShopTechnology.services.order.impl;

import com.ndh.ShopTechnology.dto.response.order.PaymentMethodResponse;
import com.ndh.ShopTechnology.repository.PaymentMethodRepository;
import com.ndh.ShopTechnology.services.order.PaymentMethodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodServiceImpl(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> listActivePaymentMethods() {
        return paymentMethodRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(PaymentMethodResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
