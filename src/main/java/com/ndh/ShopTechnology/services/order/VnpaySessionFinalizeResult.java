package com.ndh.ShopTechnology.services.order;

/**
 * Kết quả tạo đơn từ phiên checkout sau khi VNPAY IPN thành công (dùng map RspCode).
 */
public enum VnpaySessionFinalizeResult {
    /** Đã tạo mới bản ghi order + cập nhật phiên. */
    CREATED,
    /** Phiên đã xử lý trước đó (idempotent). */
    ALREADY_COMPLETED,
    /** Không tìm thấy phiên (txnRef sai / đã xóa). */
    NOT_FOUND,
    /** Phiên hết hạn hoặc dữ liệu không còn hợp lệ. */
    NOT_PAYABLE,
    /** Lỗi nghiệp vụ khi tạo đơn. */
    BUSINESS_ERROR
}
