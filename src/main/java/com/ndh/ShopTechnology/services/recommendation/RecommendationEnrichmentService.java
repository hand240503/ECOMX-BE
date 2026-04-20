package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;

import java.util.List;

public interface RecommendationEnrichmentService {

    List<ProductFullResponse> enrich(List<RecommendationItem> items);
}
