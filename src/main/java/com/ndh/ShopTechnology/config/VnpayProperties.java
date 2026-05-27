package com.ndh.ShopTechnology.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {

    private boolean enabled = true;

    private String tmnCode = "";
    private String hashSecret = "";

    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    private String returnUrl = "http://127.0.0.1:8080/api/v1/payment/vnpay/return";

    private String frontendRedirectBase = "http://localhost:5173";

    private String frontendResultPath = "/payment/vnpay-callback";

    private String paymentMethodCode = "VNPAY";

    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private int paymentMinutesToExpire = 15;

    private boolean devSimulateSuccessEnabled = false;
}
