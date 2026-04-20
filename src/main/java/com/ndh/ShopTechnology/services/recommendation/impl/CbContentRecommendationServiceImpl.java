package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.constants.RecommendationAlgorithm;
import com.ndh.ShopTechnology.dto.response.recommendation.CbContentRecommendationResponse;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.entities.recommendation.CbContentRecommendation;
import com.ndh.ShopTechnology.repository.CbContentRecommendationRepository;
import com.ndh.ShopTechnology.services.recommendation.CbContentRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CbContentRecommendationServiceImpl implements CbContentRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(CbContentRecommendationServiceImpl.class);

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT     = 100;

    private final CbContentRecommendationRepository cbContentRecommendationRepository;
    private final CbContentRecommendationServiceImpl self;

    public CbContentRecommendationServiceImpl(
            CbContentRecommendationRepository cbContentRecommendationRepository,
            @Lazy CbContentRecommendationServiceImpl self) {
        this.cbContentRecommendationRepository = cbContentRecommendationRepository;
        this.self = self;
    }

    // ─── 1. API có metadata (cho controller debug/PDP) ────────────
    @Override
    @Transactional(readOnly = true)
    public Optional<CbContentRecommendationResponse> getForUser(Long userId, int limit) {
        int safeLimit = limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        return self.findRaw(userId)
                .map(e -> CbContentRecommendationResponse.fromEntity(e, safeLimit));
    }

    // ─── 2. Check tồn tại ─────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public boolean existsForUser(Long userId) {
        return cbContentRecommendationRepository.existsByUserId(userId);
    }

    // ─── 3. API phẳng — có exclude ────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItem> getRecommendations(
            Long userId, int limit, Collection<Long> excludeIds) {

        if (userId == null) return List.of();

        int safeLimit = limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        Optional<CbContentRecommendation> opt = self.findRaw(userId);
        if (opt.isEmpty()) return List.of();

        CbContentRecommendation rec = opt.get();
        List<Long> pids = rec.getProductIds();
        List<Double> sims = rec.getSimilarities();
        if (pids == null || pids.isEmpty()) return List.of();

        Set<Long> excl = (excludeIds == null || excludeIds.isEmpty())
                ? Collections.emptySet()
                : Set.copyOf(excludeIds);

        List<RecommendationItem> out = new ArrayList<>(safeLimit);
        for (int i = 0; i < pids.size() && out.size() < safeLimit; i++) {
            Long pid = pids.get(i);
            if (excl.contains(pid)) continue;
            double score = (sims != null && i < sims.size()) ? sims.get(i) : 0.0;
            out.add(new RecommendationItem(pid, score, RecommendationAlgorithm.CONTENT_USER));
        }
        return out;
    }

    // ─── 4. API phẳng — không exclude (Hybrid dùng) ───────────────
    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItem> getRecommendations(Long userId, int limit) {
        return getRecommendations(userId, limit, null);
    }

    // ─── 5. Cached raw fetch ──────────────────────────────────────
    @Cacheable(value = "userContentRecs", key = "#userId")
    public Optional<CbContentRecommendation> findRaw(Long userId) {
        log.debug("CACHE MISS - user content recs for {}", userId);
        return cbContentRecommendationRepository.findByUserId(userId);
    }
}