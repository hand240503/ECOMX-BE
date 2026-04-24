package com.ndh.ShopTechnology.controller.payment;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.payment.VnpayCreatePaymentData;
import com.ndh.ShopTechnology.dto.response.payment.VnpayIpnResponse;
import com.ndh.ShopTechnology.services.payment.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("${api.prefix}/payment/vnpay")
public class VnpayController {

    private final VnpayService vnpayService;

    public VnpayController(VnpayService vnpayService) {
        this.vnpayService = vnpayService;
    }

    /**
     * Tạo URL chuyển hướng sang cổng VNPAY (mở tab / location.assign). {@code sessionId} = {@code checkoutSessionId}
     * trả về từ {@code POST /orders} khi PTTT = VNPAY.
     */
    @PostMapping("/checkout-sessions/{sessionId}/payment-url")
    public ResponseEntity<APIResponse<VnpayCreatePaymentData>> createPaymentUrl(
            @PathVariable long sessionId,
            HttpServletRequest request) {
        String ip = clientIp(request);
        VnpayCreatePaymentData data = vnpayService.createPaymentUrl(sessionId, ip);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "OK",
                data,
                null,
                null));
    }

    /**
     * IPN — server VNPAY gọi (GET). Cập nhật trạng thái thanh toán, phản hồi JSON theo tài liệu VNPAY.
     * Đăng ký full URL công khai (HTTPS khi production) tại trang cấu hình VNPAY.
     */
    @GetMapping("/ipn")
    @ResponseStatus(HttpStatus.OK)
    public VnpayIpnResponse ipn(HttpServletRequest request) {
        return vnpayService.handleIpn(request);
    }

    /**
     * Return — khách hàng quay lại từ VNPAY. Chỉ xác thực chữ ký, redirect về FE (DB cập nhật tại IPN).
     */
    @GetMapping("/return")
    public RedirectView vnpayReturn(HttpServletRequest request) {
        String target = vnpayService.buildReturnRedirectUrl(request);
        RedirectView v = new RedirectView(target);
        v.setHttp10Compatible(false);
        return v;
    }

    private static String clientIp(HttpServletRequest request) {
        String h = request.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) {
            return h.split(",")[0].trim();
        }
        h = request.getHeader("X-Real-IP");
        if (h != null && !h.isBlank()) {
            return h.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
