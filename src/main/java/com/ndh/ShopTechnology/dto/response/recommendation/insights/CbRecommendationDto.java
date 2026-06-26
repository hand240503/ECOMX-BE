package com.ndh.ShopTechnology.dto.response.recommendation.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gợi ý theo profile của 1 user (cb_content_recommendation) đã bung mảng product/similarity theo rank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CbRecommendationDto {

    private Long userId;
    private String userName;
    private Integer topK;
    private String computedAt;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Integer rank;
        private Long productId;
        private String productName;
        private Double similarity;
    }
}
