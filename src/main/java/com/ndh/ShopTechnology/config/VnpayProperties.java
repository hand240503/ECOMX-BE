package com.ndh.ShopTechnology.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình VNPAY (Sandbox / Production) — điền từ tài khoản merchant hoặc biến môi trường.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {

    /** Tắt tích hợp nếu chưa cấu hình hash secret. */
    private boolean enabled = true;

    private String tmnCode = "";
    private String hashSecret = "";

    /** Ví dụ sandbox: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html */
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    /**
     * URL công khai (HTTPS khi production) tới endpoint {@code /payment/vnpay/return} của backend.
     * Truyền đúng giá trị này trong tham số gửi VNPAY (vnp_ReturnUrl).
     */
    private String returnUrl = "http://127.0.0.1:8080/api/v1/payment/vnpay/return";

    /**
     * Sau khi xác thực chữ ký, redirect trình duyệt về FE (cùng tham số query VNPAY trả về).
     * Ví dụ: http://localhost:5173
     */
    private String frontendRedirectBase = "http://localhost:5173";

    /** Đường dẫn thêm trên FE, ví dụ: /payment/vnpay-callback */
    private String frontendResultPath = "/payment/vnpay-callback";

    /** Mã PTTT trong bảng {@code payment_methods} tương ứng VNPAY. */
    private String paymentMethodCode = "VNPAY";

    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private int paymentMinutesToExpire = 15;

    /**
     * Khi {@code true}: cho phép gọi API mô phỏng thanh toán thành công (tạo đơn + COMPLETED) khi chạy local mà IPN
     * VNPAY không tới được. <strong>Luôn {@code false} trên production.</strong>
     */
    private boolean devSimulateSuccessEnabled = false;
}
