package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Trạng thái giao dịch theo cột {@code vnp_TransactionStatus} tài liệu VNPAY Thanh toán Pay (Ví dụ: 00 thành công, 01
 * chưa hoàn tất, 02 lỗi). Mô tả bám bảng mã chính thức VNPAY; {@link #internalState} là trạng thái nội bộ phiên/đơn.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VnpayTransactionStatusResponse {

    String transactionPublicId;

    /**
     * Mã {@code vnp_TransactionStatus} theo tài liệu VNPAY (chuỗi 2 số, ví dụ "00", "01", "02").
     */
    String vnpTransactionStatus;

    /**
     * Mô tả tương ứng mã (tiếng Việt) — cùng ý bảng mã lỗi VNPAY.
     */
    String vnpTransactionStatusMessage;

    /**
     * Trạng thái nội bộ ứng dụng: PENDING, EXPIRED, FAILED, CANCELLED, COMPLETED.
     */
    String internalState;

    /** Có khi còn bản ghi checkout_sessions. */
    String vnpayTxnRef;

    /** Có khi thanh toán thành công. */
    Long orderId;
}
