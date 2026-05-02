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
     * Giá trị {@code checkout_sessions.public_id}: nếu FE gửi {@code order.checkoutWorkSessionId} thì trùng giá trị đó;
     * nếu không thì là chuỗi của {@link #checkoutSessionId}. Dùng cho {@code GET .../vnpay-pending/{transactionPublicId}}.
     */
    String transactionPublicId;
    /** Tổng tiền (VND) giữ cho VNPAY / hiển thị. */
    Double pendingTotal;
    /**
     * Ước tính từ địa chỉ giao (OSRM + bảng phí cứng). Có khi tạo phiên VNPAY hoặc đơn COD;
     * null nếu dịch vụ định tuyến/geocode lỗi.
     */
    Double deliveryDistanceMeters;
    Long shippingFeeVnd;
    PaymentMethodSummaryResponse paymentMethod;
    String message;
}
