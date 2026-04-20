package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.constants.RecommendationAlgorithm;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.repository.PopularityRepository;
import com.ndh.ShopTechnology.services.recommendation.PopularityService;
import com.ndh.ShopTechnology.services.recommendation.dto.PopularProductRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class PopularityServiceImpl implements PopularityService {

    private static final Logger log = LoggerFactory.getLogger(PopularityServiceImpl.class);

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT     = 100;
    private static final int FETCH_BUFFER  = 100;

    private final PopularityRepository popularityRepository;
    private final PopularityServiceImpl self;

    public PopularityServiceImpl(
            PopularityRepository popularityRepository,
            @Lazy PopularityServiceImpl self) {
        this.popularityRepository = popularityRepository;
        this.self = self;
    }

    // ─── 1. API chính: hỗ trợ exclude ─────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItem> getPopular(int limit, Collection<Long> excludeIds) {

        int safeLimit = limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        Set<Long> excl = (excludeIds == null || excludeIds.isEmpty())
                ? Collections.emptySet()
                : Set.copyOf(excludeIds);

        List<PopularProductRow> raw = self.fetchPopularRaw();
        if (raw.isEmpty()) return List.of();

        long maxCnt = Math.max(1L, raw.get(0).getCnt());

        List<RecommendationItem> out = new ArrayList<>(safeLimit);
        for (PopularProductRow r : raw) {
            if (out.size() >= safeLimit) break;
            if (excl.contains(r.getProductId())) continue;
            out.add(new RecommendationItem(
                    r.getProductId(),
                    (double) r.getCnt() / maxCnt,
                    RecommendationAlgorithm.POPULAR));
        }
        return out;
    }

    // ─── 2. Shortcut: không exclude (dùng cho Hybrid) ─────────────
    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItem> getPopularItems(int limit) {
        return getPopular(limit, null);
    }

    // ─── 3. Cached raw fetch ──────────────────────────────────────
    @Cacheable(value = "popularItems", key = "'all'")
    public List<PopularProductRow> fetchPopularRaw() {
        log.info("CACHE MISS - rebuilding popular items list");
        return popularityRepository.findTopPopular(PageRequest.of(0, FETCH_BUFFER));
    }
}