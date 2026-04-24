package com.ndh.ShopTechnology.constants;

/**
 * Trạng thái đơn hàng (status) + tiến trình trả hàng / hoàn tiền (returnRefundStatus) — tách bạch 2 mảng số.
 */
public final class OrderConstants {

    private OrderConstants() {
    }

    /** Tạo đơn: chờ xác nhận. */
    public static final int STATUS_AWAITING_CONFIRM = 1;
    /** Chờ vận chuyển (lấy hàng / giao bên VC). */
    public static final int STATUS_AWAITING_SHIPMENT = 2;
    /** Chờ giao hàng. */
    public static final int STATUS_AWAITING_DELIVERY = 3;
    /** Hoàn thành. */
    public static final int STATUS_COMPLETED = 4;
    /** Đã hủy. */
    public static final int STATUS_CANCELLED = 5;

    /** Mặc định: đơn mua online. */
    public static final int TYPE_ONLINE = 0;

    /** Chưa có yêu cầu trả / hoàn (trường returnRefundStatus = null trên entity). */
    public static final int RETURN_STATUS_NONE = 0;

    /** KH yêu cầu trả hàng / hoàn tiền. */
    public static final int RETURN_STATUS_REQUESTED = 1;
    /** Chấp nhận, chờ hàng về (hoặc xác minh). */
    public static final int RETURN_STATUS_ACCEPTED = 2;
    /** Đang xử lý hoàn tiền. */
    public static final int RETURN_STATUS_REFUNDING = 3;
    /** Đã hoàn tiền. */
    public static final int RETURN_STATUS_REFUNDED = 4;
    /** Từ chối yêu cầu. */
    public static final int RETURN_STATUS_REJECTED = 5;

    /** Số ngày (kể từ lúc ghi nhận thanh toán) trong đó còn được yêu cầu trả / hoàn. */
    public static final int RETURN_ELIGIBLE_DAYS_AFTER_PAID = 7;

    /** Mã PTTT trong bảng {@code payment_methods}: thanh toán khi giao. */
    public static final String PM_CODE_COD = "COD";

    /** Mã PTTT VNPAY (thanh toán trực tuyến, đơn tạo sau khi thanh toán thành công). */
    public static final String PM_CODE_VNPAY = "VNPAY";
}
