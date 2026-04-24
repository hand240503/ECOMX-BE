package com.ndh.ShopTechnology.config;

import com.ndh.ShopTechnology.entities.order.PaymentMethodEntity;
import com.ndh.ShopTechnology.repository.PaymentMethodRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bảo đảm có PTTT VNPAY (upsert theo mã) — cần cho DB đã từng seed trước khi thêm mã mới.
 */
@Component
@Order(2)
public class VnpayPaymentMethodEnsurer implements ApplicationRunner {

    private static final String CODE = "VNPAY";

    private final PaymentMethodRepository paymentMethodRepository;

    public VnpayPaymentMethodEnsurer(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (paymentMethodRepository.findByCode(CODE).isPresent()) {
            return;
        }
        int max = paymentMethodRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(PaymentMethodEntity::getSortOrder)
                .max(Integer::compareTo)
                .orElse(0);
        paymentMethodRepository.save(PaymentMethodEntity.builder()
                .name("VNPAY (thẻ ATM, QR, thẻ quốc tế)")
                .code(CODE)
                .active(true)
                .sortOrder(max + 1)
                .build());
    }
}
