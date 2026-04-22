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

    /**
     * Gợi ý theo <strong>một</strong> sản phẩm: trộn có trọng số CF item–item + content item–item
     * (không dùng session/recent profile). Khác {@link #getSimilarToProduct} — bên đó là cascade
     * (ưu tiên CF đủ slot, thiếu mới bổ sung content).
     */
    List<RecommendationItem> getItemHybridSimilar(Integer productId, int limit);

    List<RecommendationItem> getPostPurchaseRecommendations(
            Integer productId, Long userId, String sessionId, int limit);
}