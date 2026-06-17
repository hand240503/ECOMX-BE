package com.ndh.ShopTechnology.services.payment;

/**
 * Kết quả thô khi gọi API truy vấn giao dịch (querydr) của VNPAY.
 *
 * @param callOk            gọi HTTP + parse JSON thành công
 * @param responseCode      vnp_ResponseCode — kết quả của API truy vấn (không phải của giao dịch).
 *                          "00" = truy vấn thành công; "91" = không tìm thấy giao dịch; ...
 * @param message           vnp_Message mô tả
 * @param transactionStatus vnp_TransactionStatus — trạng thái thật của giao dịch.
 *                          "00" = thành công, "01" = chưa hoàn tất, "02" = lỗi
 * @param amount            vnp_Amount (đã nhân 100), null nếu thiếu
 * @param signatureValid    chữ ký phản hồi có khớp không
 */
public record VnpayQueryDrResponse(
        boolean callOk,
        String responseCode,
        String message,
        String transactionStatus,
        Long amount,
        boolean signatureValid) {

    public static VnpayQueryDrResponse failedCall(String message) {
        return new VnpayQueryDrResponse(false, null, message, null, null, false);
    }
}
