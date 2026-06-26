package com.ndh.ShopTechnology.dto.response.recommendation.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Số liệu tổng quan của pipeline gợi ý (đọc các bảng do recsys_builder sinh ra).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsSummaryDto {

    /** Số cặp item_similarity thuật toán cf_cosine. */
    private long cfCosinePairs;

    /** Số cặp item_similarity thuật toán content_tfidf. */
    private long contentTfidfPairs;

    /** Số dòng implicit rating (user_ratings type = 1). */
    private long implicitRatings;

    /** Số user khác nhau có implicit rating. */
    private long implicitUsers;

    /** Số sản phẩm khác nhau có implicit rating. */
    private long implicitProducts;

    /** Số user có user_preference_profile. */
    private long userProfiles;

    /** Số dòng gợi ý theo profile (cb_content_recommendation). */
    private long cbRecommendations;

    /** Số sản phẩm đang bán (products.status = 1). */
    private long activeProducts;
}
