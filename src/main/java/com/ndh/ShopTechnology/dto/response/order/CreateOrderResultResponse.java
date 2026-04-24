package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Kết quả tạo đơn: COD = đã có {@link #order};
 * VNPAY = chỉ tạo phiên checkout, đơn thật sau khi VNPAY IPN thành công.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderResultResponse {

    /**
     * {@code ORDER_CREATED} — đã lưu bảng orders; {@code PENDING_VNPAY_PAYMENT} — chưa có đơn, chỉ có phiên VNPAY.
     */
    String outcome;
    /** Có khi {@code outcome = ORDER_CREATED}. */
    OrderResponse order;
    /** Có khi PTTT = VNPAY, phiên chưa thanh toán. Gọi {@code POST .../payment/vnpay/checkout-sessions/{id}/payment-url}. */
    Long checkoutSessionId;
    /**
     * UUID công khai: {@code GET .../orders/vnpay-pending/{transactionPublicId} } để xem thất bại / hết hạn / đơn đã tạo
     * (khi thành công, phiên DB bị xóa nhưng api vẫn trả {@code state=COMPLETED} nếu đã có đơn liên kết).
     */
    String transactionPublicId;
    /** Tổng tiền (VND) giữ cho VNPAY / hiển thị. */
    Double pendingTotal;
    PaymentMethodSummaryResponse paymentMethod;
    String message;
}
