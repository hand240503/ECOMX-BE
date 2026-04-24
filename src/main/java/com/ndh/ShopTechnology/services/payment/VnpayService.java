package com.ndh.ShopTechnology.services.payment;

import com.ndh.ShopTechnology.dto.response.payment.VnpayCreatePaymentData;
import com.ndh.ShopTechnology.dto.response.payment.VnpayIpnResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface VnpayService {

    /** @param checkoutSessionId id phiên {@link com.ndh.ShopTechnology.entities.order.CheckoutSessionEntity} (sau POST /orders với VNPAY). */
    VnpayCreatePaymentData createPaymentUrl(long checkoutSessionId, String clientIp);

    VnpayIpnResponse handleIpn(HttpServletRequest request);

    /**
     * Chỉ dùng cho ReturnUrl: xác thực chữ ký (không cập nhật DB theo tài liệu VNPAY).
     */
    String buildReturnRedirectUrl(HttpServletRequest request);
}
