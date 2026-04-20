package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;

import java.util.List;

public interface HybridRecommendationService {

    List<RecommendationItem> getHomeRecommendations(
            Long userId, String sessionId, int limit);

    List<RecommendationItem> getSimilarToProduct(
            Integer productId, Long userId, String sessionId, int limit);

    List<RecommendationItem> getPostPurchaseRecommendations(
            Integer productId, Long userId, String sessionId, int limit);
}