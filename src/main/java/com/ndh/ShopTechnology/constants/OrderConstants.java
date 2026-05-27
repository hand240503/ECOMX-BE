package com.ndh.ShopTechnology.constants;

public final class OrderConstants {

    private OrderConstants() {
    }

    public static final int STATUS_AWAITING_CONFIRM = 1;
    public static final int STATUS_AWAITING_SHIPMENT = 2;
    public static final int STATUS_AWAITING_DELIVERY = 3;
    public static final int STATUS_COMPLETED = 4;
    public static final int STATUS_CANCELLED = 5;

    public static final int TYPE_ONLINE = 0;

    public static final int RETURN_STATUS_NONE = 0;

    public static final int RETURN_STATUS_REQUESTED = 1;
    public static final int RETURN_STATUS_ACCEPTED = 2;
    public static final int RETURN_STATUS_REFUNDING = 3;
    public static final int RETURN_STATUS_REFUNDED = 4;
    public static final int RETURN_STATUS_REJECTED = 5;

    public static final int RETURN_ELIGIBLE_DAYS_AFTER_PAID = 7;

    public static final String PM_CODE_COD = "COD";

    public static final String PM_CODE_VNPAY = "VNPAY";
}
