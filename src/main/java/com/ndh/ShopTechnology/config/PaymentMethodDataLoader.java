package com.ndh.ShopTechnology.config;

import com.ndh.ShopTechnology.entities.order.PaymentMethodEntity;
import com.ndh.ShopTechnology.repository.PaymentMethodRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bảo đảm có vài bản ghi PTTT mặc định (chỉ chạy khi bảng còn trống).
 */
@Component
@Order(1)
public class PaymentMethodDataLoader implements ApplicationRunner {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodDataLoader(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (paymentMethodRepository.count() > 0) {
            return;
        }
        paymentMethodRepository.save(PaymentMethodEntity.builder()
                .name("Thanh toán khi nhận hàng (COD)")
                .code("COD")
                .active(true)
                .sortOrder(0)
                .build());
        paymentMethodRepository.save(PaymentMethodEntity.builder()
                .name("Chuyển khoản ngân hàng")
                .code("BANK_TRANSFER")
                .active(true)
                .sortOrder(1)
                .build());
        paymentMethodRepository.save(PaymentMethodEntity.builder()
                .name("Ví điện tử (MoMo, ZaloPay, …)")
                .code("E_WALLET")
                .active(true)
                .sortOrder(2)
                .build());
    }
}
