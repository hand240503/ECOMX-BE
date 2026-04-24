package com.ndh.ShopTechnology.controller.order;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.order.PaymentMethodResponse;
import com.ndh.ShopTechnology.services.order.PaymentMethodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<PaymentMethodResponse>>> list() {
        List<PaymentMethodResponse> data = paymentMethodService.listActivePaymentMethods();
        return ResponseEntity.ok(APIResponse.of(
                true,
                "OK",
                data,
                null,
                null));
    }
}
