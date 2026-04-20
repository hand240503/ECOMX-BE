package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.dto.response.recommendation.CbContentRecommendationResponse;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CbContentRecommendationService {

    Optional<CbContentRecommendationResponse> getForUser(Long userId, int limit);

    boolean existsForUser(Long userId);

    /** API "phẳng" để Hybrid service dùng — tự lọc excludeIds. */
    List<RecommendationItem> getRecommendations(
            Long userId, int limit, Collection<Long> excludeIds);

    List<RecommendationItem> getRecommendations(Long userId, int limit);
}