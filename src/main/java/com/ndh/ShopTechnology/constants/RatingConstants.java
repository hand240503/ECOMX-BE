package com.ndh.ShopTechnology.constants;

/** Hằng số loại đánh giá người dùng (cột user_ratings.type). */
public final class RatingConstants {

    private RatingConstants() {
    }

    /**
     * Đánh giá explicit: người dùng chủ động chấm sao (1–5) cho sản phẩm đã mua.
     * Khớp recommend builder (refactored/queries.py EXPLICIT_RATINGS_SQL): type = 0 hoặc NULL.
     * Lưu nguyên thang 1–5; builder tự nhân RATING_MULTIPLIER (2.0) để đưa về thang 0–10.
     */
    public static final int TYPE_EXPLICIT = 0;

    /**
     * Đánh giá implicit (CF tự sinh từ hành vi): type = 1, thang 0–10.
     * Do builder quản lý (DELETE WHERE type=1 rồi dựng lại) — backend KHÔNG ghi loại này.
     */
    public static final int TYPE_IMPLICIT = 1;
}
