package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

/**
 * Trạng thái thanh toán VNPAY theo {@code publicId} phiên (do {@link CreateOrderResultResponse} trả về khi
 * PENDING). Sau khi thanh toán thành công, bản ghi phiên còn ở DB (COMPLETED + order_id) và {@code state = COMPLETED}
 * cùng {@link #order}.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VnpayPendingTransactionResponse {

    /** Cùng giá trị lưu trên bảng checkout_sessions (và lặp lại trên order sau khi thành công) để FE đối chiếu. */
    String transactionPublicId;

    /**
     * PENDING, EXPIRED, FAILED, CANCELLED (hủy từ FE), hoặc COMPLETED (đã có đơn; phiên vẫn lưu lịch sử trên
     * {@code checkout_sessions}).
     */
    String state;

    Date expiresAt;
    Double total;
    PaymentMethodSummaryResponse paymentMethod;

    /** Số dùng làm {@code vnp_TxnRef} lúc tạo URL thanh toán — hỗ trợ/đối soát, không cần hiển thị end-user. */
    String vnpayTxnRef;

    /**
     * Thông tin đơn: snapshot từ payload phiên (PENDING/FAILED/EXPIRED) hoặc từ đơn đã lưu (COMPLETED) —
     * giúp FE hiển thị giỏ / địa chỉ mà không phụ thuộc chỉ vào trường {@link #order}.
     */
    VnpayCheckoutOrderInfoResponse orderInfo;

    /**
     * Đủ cấu trúc {@link OrderResponse} (audit, PTTT, trạng thái đơn) khi {@code state = COMPLETED}; có thể
     * trùng nội dung cốt lõi với {@link #orderInfo}.
     */
    OrderResponse order;

    String message;
}
