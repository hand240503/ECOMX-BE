package com.ndh.ShopTechnology.services.payment;

import com.ndh.ShopTechnology.dto.response.payment.VnpayCreatePaymentData;
import com.ndh.ShopTechnology.dto.response.payment.VnpayIpnResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface VnpayService {

    VnpayCreatePaymentData createPaymentUrl(long checkoutSessionId, String clientIp);

    VnpayIpnResponse handleIpn(HttpServletRequest request);

    String buildReturnRedirectUrl(HttpServletRequest request);
}
