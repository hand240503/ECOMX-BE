package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.recommendation.dto.SessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionBooster {

    private final ProductRepository productRepository;

    /** Boost tối đa cho category: x1 -> x2.0 tuỳ weight relative */
    private static final double CATEGORY_BOOST_RANGE = 1.0;

    /** Boost tối đa cho brand: x1 -> x1.5 tuỳ weight relative */
    private static final double BRAND_BOOST_RANGE = 0.5;

    public List<RecommendationItem> boost(
            List<RecommendationItem> baseRecs,
            SessionContext ctx) {

        if (baseRecs == null || baseRecs.isEmpty()) return List.of();
        if (ctx == null || ctx.isEmpty()) return baseRecs;

        List<Long> productIds = baseRecs.stream()
                .map(RecommendationItem::getProductId)
                .toList();

        Map<Long, ProductEntity> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        double maxCatW = ctx.getMaxCategoryWeight();
        double maxBrW  = ctx.getMaxBrandWeight();

        return baseRecs.stream()
                // Loại sản phẩm đã xem trong session
                .filter(r -> !ctx.getSessionProductIds().contains(r.getProductId()))
                .map(r -> {
                    ProductEntity p = productMap.get(r.getProductId());
                    double boost = computeBoost(p, ctx, maxCatW, maxBrW);
                    String source = boost > 1.05
                            ? r.getSource() + "+session"
                            : r.getSource();
                    return new RecommendationItem(
                            r.getProductId(),
                            r.getScore() * boost,
                            source);
                })
                .sorted(Comparator.comparingDouble(RecommendationItem::getScore).reversed())
                .toList();
    }

    private double computeBoost(
            ProductEntity product,
            SessionContext ctx,
            double maxCatW,
            double maxBrW) {

        if (product == null) return 1.0;

        double boost = 1.0;

        // Category boost — tỷ lệ với weight relative (0..1)
        if (product.getCategory() != null) {
            double catW = ctx.getCategoryWeight(product.getCategory().getId());
            if (catW > 0 && maxCatW > 0) {
                boost *= 1.0 + (catW / maxCatW) * CATEGORY_BOOST_RANGE;
            }
        }

        // Brand boost
        if (product.getBrand() != null) {
            double brW = ctx.getBrandWeight(product.getBrand().getName());
            if (brW > 0 && maxBrW > 0) {
                boost *= 1.0 + (brW / maxBrW) * BRAND_BOOST_RANGE;
            }
        }

        return boost;
    }
}