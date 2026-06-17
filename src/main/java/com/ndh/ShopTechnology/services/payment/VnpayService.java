package com.ndh.ShopTechnology.services.payment;

import com.ndh.ShopTechnology.dto.response.payment.VnpayCreatePaymentData;
import com.ndh.ShopTechnology.dto.response.payment.VnpayIpnResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface VnpayService {

    VnpayCreatePaymentData createPaymentUrl(long checkoutSessionId, String clientIp);

    VnpayIpnResponse handleIpn(HttpServletRequest request);

    String buildReturnRedirectUrl(HttpServletRequest request);

    /**
     * Chủ động gọi querydr để đối soát một phiên checkout còn PENDING (khi IPN chưa/không tới).
     * Nếu VNPAY báo thành công thì finalize tạo đơn; nếu báo lỗi thì đánh dấu FAILED;
     * nếu chưa kết luận được thì giữ nguyên PENDING. An toàn để gọi lặp (idempotent).
     */
    void reconcilePendingCheckoutSession(long checkoutSessionId);
}
