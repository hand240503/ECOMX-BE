package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;

import java.util.Collection;
import java.util.List;

public interface ContentItemRecommendationService {
    List<RecommendationItem> getSimilar(
            Integer productId, int limit, Collection<Integer> excludeIds);

}