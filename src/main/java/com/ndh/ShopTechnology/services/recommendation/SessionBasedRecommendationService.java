package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.dto.request.recommendation.SessionProfileRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;

import java.util.List;

public interface SessionBasedRecommendationService {

    List<ProductFullResponse> recommendForSession(SessionProfileRequest profile);
}
