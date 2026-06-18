package com.ndh.ShopTechnology.constants;

/** Hằng số loại đánh giá người dùng (cột user_ratings.type). */
public final class RatingConstants {

    private RatingConstants() {
    }

    /** Đánh giá explicit: người dùng chủ động chấm sao (1–5) cho sản phẩm đã mua. */
    public static final int TYPE_EXPLICIT = 1;

    /** Đánh giá implicit: suy ra từ hành vi (xem, mua…), không do người dùng chấm trực tiếp. */
    public static final int TYPE_IMPLICIT = 0;
}
