package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.request.recommendation.SessionProfileRequest;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.services.recommendation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionBasedRecommendationServiceImpl
        implements SessionBasedRecommendationService {

    private final CfRecommendationService cfService;
    private final ContentItemRecommendationService contentItemService;
    private final RecommendationEnrichmentService enrichmentService;
    private final PopularityService popularityService;

    private static final int DEFAULT_LIMIT  = 20;
    private static final int MAX_LIMIT      = 50;
    private static final int CANDIDATES_PER_SEED = 20;
    private static final double W_CF        = 0.7;
    private static final double W_CONTENT   = 0.3;

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> recommendForSession(
            SessionProfileRequest profile) {

        int limit = (profile.getLimit() != null && profile.getLimit() > 0)
                ? Math.min(profile.getLimit(), MAX_LIMIT)
                : DEFAULT_LIMIT;

        List<Long> recent = profile.getRecentProductIds();
        if (recent == null || recent.isEmpty()) {
            return enrichmentService.enrich(
                    popularityService.getPopular(limit, null));
        }

        Set<Integer> excludeInt = new HashSet<>();
        recent.forEach(id -> excludeInt.add(id.intValue()));
        if (profile.getCartProductIds() != null) {
            profile.getCartProductIds().forEach(id -> excludeInt.add(id.intValue()));
        }

        Map<Long, Double> scoreMap = new HashMap<>();
        Map<Long, String> sourceMap = new HashMap<>();

        for (int i = 0; i < recent.size(); i++) {
            Long seedId = recent.get(i);
            double recencyWeight = 1.0 / (1.0 + i);

            List<RecommendationItem> cfRecs = cfService.getSimilar(
                    seedId.intValue(), CANDIDATES_PER_SEED, excludeInt);
            cfRecs.forEach(r -> {
                double w = r.getScore() * W_CF * recencyWeight;
                scoreMap.merge(r.getProductId(), w, Double::sum);
                sourceMap.merge(r.getProductId(), "cf",
                        (oldS, newS) -> oldS.contains(newS) ? oldS : oldS + "+" + newS);
            });

            List<RecommendationItem> contentRecs = contentItemService.getSimilar(
                    seedId.intValue(), CANDIDATES_PER_SEED, excludeInt);
            contentRecs.forEach(r -> {
                double w = r.getScore() * W_CONTENT * recencyWeight;
                scoreMap.merge(r.getProductId(), w, Double::sum);
                sourceMap.merge(r.getProductId(), "content",
                        (oldS, newS) -> oldS.contains(newS) ? oldS : oldS + "+" + newS);
            });
        }

        if (scoreMap.isEmpty()) {
            log.warn("Session {} không tìm thấy similar nào → fallback popular",
                    profile.getSessionId());
            return enrichmentService.enrich(
                    popularityService.getPopular(limit, null));
        }

        List<RecommendationItem> sorted = scoreMap.entrySet().stream()
                .map(e -> new RecommendationItem(
                        e.getKey(),
                        e.getValue(),
                        sourceMap.getOrDefault(e.getKey(), "session")))
                .sorted(Comparator.comparingDouble(RecommendationItem::getScore).reversed())
                .limit(limit)
                .toList();

        log.debug("Session {} → {} candidates → top {}",
                profile.getSessionId(), scoreMap.size(), sorted.size());

        return enrichmentService.enrich(sorted);
    }
}
