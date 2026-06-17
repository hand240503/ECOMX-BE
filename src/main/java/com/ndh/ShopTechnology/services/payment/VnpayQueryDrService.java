package com.ndh.ShopTechnology.services.payment;

import java.util.Date;

public interface VnpayQueryDrService {

    /**
     * Gọi API querydr của VNPAY để truy vấn kết quả một giao dịch.
     *
     * @param txnRef          mã tham chiếu giao dịch (vnp_TxnRef) đã gửi sang VNPAY
     * @param transactionDate thời điểm phát sinh giao dịch (lúc tạo URL thanh toán)
     * @param clientIp        IP gửi kèm yêu cầu truy vấn
     */
    VnpayQueryDrResponse query(String txnRef, Date transactionDate, String clientIp);
}
