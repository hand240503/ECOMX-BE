package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.config.RecommendationBlendProperties;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.enums.UserState;
import com.ndh.ShopTechnology.repository.CollectorLogRepository;
import com.ndh.ShopTechnology.services.recommendation.CbContentRecommendationService;
import com.ndh.ShopTechnology.services.recommendation.CfRecommendationService;
import com.ndh.ShopTechnology.services.recommendation.ContentItemRecommendationService;
import com.ndh.ShopTechnology.services.recommendation.HybridRecommendationService;
import com.ndh.ShopTechnology.services.recommendation.PopularityService;
import com.ndh.ShopTechnology.services.recommendation.SessionContextService;
import com.ndh.ShopTechnology.services.recommendation.UserStateService;
import com.ndh.ShopTechnology.services.recommendation.dto.SessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRecommendationServiceImpl implements HybridRecommendationService {

    private final UserStateService userStateService;
    private final CfRecommendationService cfService;
    private final ContentItemRecommendationService contentItemService;
    private final CbContentRecommendationService cbContentService;
    private final PopularityService popularityService;
    private final CollectorLogRepository collectorLogRepository;
    private final SessionContextService sessionContextService;
    private final SessionBooster sessionBooster;
    private final RecommendationBlendProperties blendProperties;

    private static final double W_CF_ITEM = 0.7;
    private static final double W_CONTENT_ITEM = 0.3;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItem> getHomeRecommendations(Long userId, String sessionId, int limit) {
        UserState state = userStateService.getUserState(userId);
        log.info("[Hybrid Home] userId={} sessionId={} state={}", userId, sessionId, state);

        Map<Supplier<List<RecommendationItem>>, Double> sources = new LinkedHashMap<>();
        switch (state) {
            case NEW -> sources.put(() -> popularityService.getPopularItems(limit * 2), 1.0);
            case COLD -> {
                sources.put(() -> cbContentService.getRecommendations(userId, limit * 2), 0.7);
                sources.put(() -> popularityService.getPopularItems(limit * 2), 0.3);
            }
            case ACTIVE -> {
                sources.put(() -> cbContentService.getRecommendations(userId, limit * 2), 0.5);
                sources.put(() -> contentToCfFromUserHistory(userId, sessionId, limit * 2), 0.3);
                sources.put(() -> popularityService.getPopularItems(limit * 2), 0.2);
            }
        }

        List<RecommendationItem> base = weightedMerge(sources, limit * 2);
        SessionContext ctx = sessionContextService.buildContext(userId);
        List<RecommendationItem> boosted = sessionBooster.boost(base, ctx);

        log.info("[Hybrid Home] base={} boosted={} blend long={} short={}",
                base.size(), boosted.size(),
                blendProperties.getLongWeight(), blendProperties.getShortWeight());

        return blendLongShort(base, boosted, limit);
    }

    /**
     * long = chuẩn hoá score trước session; short = chuẩn hoá score sau session boost.
     */
    private List<RecommendationItem> blendLongShort(
            List<RecommendationItem> base,
            List<RecommendationItem> boosted,
            int limit) {

        if (boosted == null || boosted.isEmpty()) {
            return truncate(base, limit);
        }
        if (base == null || base.isEmpty()) {
            return boosted.stream().limit(limit).toList();
        }

        Map<Long, Double> longNorm = minMaxNormByProductId(base);
        Map<Long, Double> shortNorm = minMaxNormByProductId(boosted);
        double lw = blendProperties.getLongWeight();
        double sw = blendProperties.getShortWeight();

        LinkedHashSet<Long> ordered = new LinkedHashSet<>();
        boosted.forEach(b -> ordered.add(b.getProductId()));
        base.forEach(b -> ordered.add(b.getProductId()));

        Map<Long, String> sourceByPid = new HashMap<>();
        boosted.forEach(b -> sourceByPid.putIfAbsent(b.getProductId(), b.getSource()));
        base.forEach(b -> sourceByPid.putIfAbsent(b.getProductId(), b.getSource()));

        return ordered.stream()
                .map(pid -> {
                    double nl = longNorm.getOrDefault(pid, 0.0);
                    double ns = shortNorm.getOrDefault(pid, 0.0);
                    double finalScore = lw * nl + sw * ns;
                    String src = sourceByPid.getOrDefault(pid, "blend");
                    return new RecommendationItem(pid, finalScore, src + "+blend");
                })
                .sorted(Comparator.comparingDouble(RecommendationItem::getScore).reversed())
                .limit(limit)
                .toList();
    }

    private static List<RecommendationItem> truncate(List<RecommendationItem> list, int limit) {
        if (list == null) return List.of();
        return list.stream().limit(limit).toList();
    }

    private static Map<Long, Double> minMaxNormByProductId(List<RecommendationItem> items) {
        if (items == null || items.isEmpty()) return Map.of();
        double min = items.stream().mapToDouble(RecommendationItem::getScore).min().orElse(0);
        double max = items.stream().mapToDouble(RecommendationItem::getScore).max().orElse(1);
        double den = Math.max(1e-9, max - min);
        return items.stream()
                .collect(Collectors.toMap(
                        RecommendationItem::getProductId,
                        it -> (it.getScore() - min) / den,
                        (a, b) -> a));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItem> getSimilarToProduct(
            Integer productId, Long userId, String sessionId, int limit) {

        List<RecommendationItem> cf = cfService.getSimilar(productId, limit, null);
        if (cf.size() >= limit) {
            return cf;
        }
        return cascade(cf,
                () -> contentItemService.getSimilar(productId, limit, null),
                limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItem> getPostPurchaseRecommendations(
            Integer productId, Long userId, String sessionId, int limit) {

        return weightedMerge(
                cfService.getSimilar(productId, limit * 2, null), W_CF_ITEM,
                contentItemService.getSimilar(productId, limit * 2, null), W_CONTENT_ITEM,
                limit);
    }

    private List<RecommendationItem> weightedMerge(
            List<RecommendationItem> listA, double wA,
            List<RecommendationItem> listB, double wB,
            int limit) {

        Map<Supplier<List<RecommendationItem>>, Double> map = new LinkedHashMap<>();
        map.put(() -> listA, wA);
        map.put(() -> listB, wB);
        return weightedMerge(map, limit);
    }

    private List<RecommendationItem> weightedMerge(
            Map<Supplier<List<RecommendationItem>>, Double> sources,
            int limit) {

        Map<Long, Double> scoreMap = new HashMap<>();
        Map<Long, String> sourceMap = new HashMap<>();

        for (Map.Entry<Supplier<List<RecommendationItem>>, Double> e : sources.entrySet()) {
            double weight = e.getValue();
            List<RecommendationItem> items;
            try {
                items = e.getKey().get();
            } catch (Exception ex) {
                log.warn("[Hybrid] Source failed, skipping: {}", ex.getMessage());
                continue;
            }
            if (items == null || items.isEmpty()) {
                continue;
            }
            for (RecommendationItem it : items) {
                Long pid = it.getProductId();
                scoreMap.merge(pid, it.getScore() * weight, Double::sum);
                sourceMap.merge(pid, it.getSource(),
                        (oldS, newS) -> oldS.contains(newS) ? oldS : oldS + "+" + newS);
            }
        }

        return scoreMap.entrySet().stream()
                .map(e -> new RecommendationItem(
                        e.getKey(), e.getValue(), sourceMap.get(e.getKey())))
                .sorted(Comparator.comparingDouble(RecommendationItem::getScore).reversed())
                .limit(limit)
                .toList();
    }

    private List<RecommendationItem> cascade(
            List<RecommendationItem> primary,
            Supplier<List<RecommendationItem>> fallback,
            int limit) {

        if (primary.size() >= limit) {
            return primary.stream().limit(limit).toList();
        }

        Set<Long> seen = primary.stream()
                .map(RecommendationItem::getProductId)
                .collect(Collectors.toSet());

        List<RecommendationItem> out = new java.util.ArrayList<>(primary);
        for (RecommendationItem it : fallback.get()) {
            if (out.size() >= limit) {
                break;
            }
            if (seen.add(it.getProductId())) {
                out.add(it);
            }
        }
        return out;
    }

    private List<RecommendationItem> contentToCfFromUserHistory(
            Long userId, String sessionId, int limit) {

        if (userId == null || userId <= 0) {
            return List.of();
        }

        List<Long> recent = collectorLogRepository
                .findRecentProductIdsByUser(userId, sessionId, 5);

        if (recent.isEmpty()) {
            return List.of();
        }

        List<Integer> recentInt = recent.stream()
                .map(Long::intValue)
                .toList();

        return recent.stream()
                .flatMap(pid -> cfService
                        .getSimilar(pid.intValue(), limit, recentInt)
                        .stream())
                .collect(Collectors.toMap(
                        RecommendationItem::getProductId,
                        it -> it,
                        (a, b) -> a.getScore() >= b.getScore() ? a : b))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(RecommendationItem::getScore).reversed())
                .limit(limit)
                .toList();
    }
}