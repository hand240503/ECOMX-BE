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

    /** Endpoint truy vấn kết quả giao dịch (querydr) / hoàn tiền (refund) của VNPAY. */
    private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    /**
     * Cửa sổ thời gian (giây) BE chủ động gọi querydr để đối soát một phiên PENDING,
     * tính từ lúc tạo phiên. Hết cửa sổ này BE ngừng truy vấn (không treo vô hạn).
     */
    private int reconcileWindowSeconds = 120;

    /**
     * Khoảng cách tối thiểu (ms) giữa 2 lần gọi querydr cho CÙNG một giao dịch.
     * VNPAY chặn query quá dày (trả vnp_ResponseCode=94 "Request is duplicated"),
     * nên cần giãn ra (mặc định 30s).
     */
    private long reconcileMinIntervalMs = 30_000;

    /**
     * Bắt buộc chữ ký phản hồi querydr hợp lệ mới chấp nhận kết quả.
     * Mặc định false: phản hồi đến qua HTTPS từ endpoint VNPAY đã biết + request đã ký,
     * nên chỉ log cảnh báo nếu chữ ký không khớp (tránh chặn nhầm giao dịch thật ở sandbox).
     */
    private boolean queryDrRequireValidSignature = false;

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
