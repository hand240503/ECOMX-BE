package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.config.RecommendationDiversityProperties;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationDiversityService {

    private static final long NO_BRAND_KEY = Long.MIN_VALUE;

    private final RecommendationDiversityProperties properties;
    private final ProductRepository productRepository;

    /**
     * Ưu tiên điểm: nhận item nếu hãng chưa đạt {@code maxPerBrand}; phần vượt nối cuối (cùng thứ tự điểm trong nhóm overflow).
     */
    public List<RecommendationItem> diversifyByBrand(List<RecommendationItem> ranked, int limit) {
        if (ranked == null || ranked.isEmpty()) {
            return List.of();
        }
        if (properties.getMaxPerBrand() < 1) {
            return ranked.stream().limit(limit).toList();
        }

        int maxPerBrand = properties.getMaxPerBrand();
        List<Long> ids = ranked.stream()
                .map(RecommendationItem::getProductId)
                .distinct()
                .toList();
        Map<Long, Long> productIdToBrandId = loadBrandIdByProductId(ids);

        List<RecommendationItem> primary = new ArrayList<>();
        List<RecommendationItem> overflow = new ArrayList<>();
        Map<Long, Integer> counts = new HashMap<>();

        for (RecommendationItem it : ranked) {
            long bKey = brandKey(productIdToBrandId.get(it.getProductId()));
            if (counts.getOrDefault(bKey, 0) < maxPerBrand) {
                primary.add(it);
                counts.merge(bKey, 1, Integer::sum);
            } else {
                overflow.add(it);
            }
        }

        List<RecommendationItem> merged = new ArrayList<>(primary.size() + overflow.size());
        merged.addAll(primary);
        merged.addAll(overflow);

        if (log.isDebugEnabled() && !overflow.isEmpty()) {
            log.debug("[Diversity] maxPerBrand={} primary={} overflow={}",
                    maxPerBrand, primary.size(), overflow.size());
        }
        return merged.stream().limit(limit).toList();
    }

    private Map<Long, Long> loadBrandIdByProductId(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllWithBrandByIdIn(ids).stream()
                .collect(Collectors.toMap(
                        ProductEntity::getId,
                        p -> p.getBrand() != null ? p.getBrand().getId() : null));
    }

    private static long brandKey(Long brandId) {
        return brandId == null ? NO_BRAND_KEY : brandId;
    }
}
