package com.ndh.ShopTechnology.services.recommendation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@Builder
public class SessionContext {

    private Long userId;

    private Set<Long> sessionProductIds;

    private Map<Long, Double> categoryWeights;

    private Map<String, Double> brandWeights;

    public boolean isEmpty() {
        return sessionProductIds == null || sessionProductIds.isEmpty();
    }

    public double getCategoryWeight(Long categoryId) {
        if (categoryId == null || categoryWeights == null) return 0.0;
        return categoryWeights.getOrDefault(categoryId, 0.0);
    }

    public double getBrandWeight(String brand) {
        if (brand == null || brandWeights == null) return 0.0;
        return brandWeights.getOrDefault(brand, 0.0);
    }

    public double getMaxCategoryWeight() {
        if (categoryWeights == null || categoryWeights.isEmpty()) return 1.0;
        return categoryWeights.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
    }

    public double getMaxBrandWeight() {
        if (brandWeights == null || brandWeights.isEmpty()) return 1.0;
        return brandWeights.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
    }
}
