package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;

import java.util.Collection;
import java.util.List;

public interface PopularityService {
    List<RecommendationItem> getPopular(int limit, Collection<Long> excludeIds);

    List<RecommendationItem> getPopularItems(int limit);
}