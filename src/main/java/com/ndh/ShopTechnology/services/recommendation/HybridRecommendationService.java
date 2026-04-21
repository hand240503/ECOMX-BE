package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;

import java.util.List;

public interface HybridRecommendationService {

    /**
     * Home feed: slice [{@code offset}, {@code offset} + {@code limit}) of the global ranked list.
     * First page: {@code offset=0}, {@code limit=20}; infinite scroll: increase {@code offset} by {@code limit}.
     */
    List<RecommendationItem> getHomeRecommendations(
            Long userId, String sessionId, int offset, int limit);

    List<RecommendationItem> getSimilarToProduct(
            Integer productId, Long userId, String sessionId, int limit);

    List<RecommendationItem> getPostPurchaseRecommendations(
            Integer productId, Long userId, String sessionId, int limit);
}