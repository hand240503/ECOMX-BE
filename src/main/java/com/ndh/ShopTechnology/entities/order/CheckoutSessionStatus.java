package com.ndh.ShopTechnology.entities.order;

public enum CheckoutSessionStatus {
    PENDING,
    /**
     * Hết hạn: có thể gán bởi IPN, hoặc vẫn lưu PENDING và hiển thị EXPIRED ở API khi {@code now > expiresAt}.
     */
    EXPIRED,
    /** Thanh toán thất bại theo thông tin IPN từ VNPAY. */
    FAILED,
    /** Người dùng hủy phiên (chưa thanh toán) — cập nhật, không xóa bản ghi. */
    CANCELLED,
    /** Thanh toán thành công; {@link com.ndh.ShopTechnology.entities.order.CheckoutSessionEntity#getOrderId()} gắn id đơn. */
    COMPLETED
}
