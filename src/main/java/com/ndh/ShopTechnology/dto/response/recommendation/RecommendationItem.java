package com.ndh.ShopTechnology.dto.response.recommendation;

import com.ndh.ShopTechnology.entities.recommendation.ItemSimilarityEntity;

public class RecommendationItem {

    private Long productId;
    private Double score;
    private String source;

    public RecommendationItem() {}

    public RecommendationItem(Long productId, Double score, String source) {
        this.productId = productId;
        this.score = score;
        this.source = source;
    }

    public static RecommendationItem fromItemSimilarity(ItemSimilarityEntity e) {
        return new RecommendationItem(
                e.getTarget().longValue(),
                e.getSimilarity().doubleValue(),
                e.getAlgorithm()
        );
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}