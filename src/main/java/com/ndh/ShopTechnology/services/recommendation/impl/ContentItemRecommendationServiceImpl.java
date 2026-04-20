package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.constants.RecommendationAlgorithm;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.entities.recommendation.ItemSimilarityEntity;
import com.ndh.ShopTechnology.repository.ItemSimilarityRepository;
import com.ndh.ShopTechnology.services.recommendation.ContentItemRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ContentItemRecommendationServiceImpl implements ContentItemRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ContentItemRecommendationServiceImpl.class);

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT     = 100;
    private static final int FETCH_BUFFER  = 50;

    private final ItemSimilarityRepository itemSimilarityRepository;
    private final ContentItemRecommendationServiceImpl self;

    public ContentItemRecommendationServiceImpl(
            ItemSimilarityRepository itemSimilarityRepository,
            @Lazy ContentItemRecommendationServiceImpl self) {
        this.itemSimilarityRepository = itemSimilarityRepository;
        this.self = self;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationItem> getSimilar(
            Integer productId, int limit, Collection<Integer> excludeIds) {

        if (productId == null) return List.of();

        int safeLimit = limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        Set<Integer> excl = (excludeIds == null || excludeIds.isEmpty())
                ? Collections.emptySet()
                : Set.copyOf(excludeIds);

        List<ItemSimilarityEntity> raw = self.fetchTopBySource(productId);

        return raw.stream()
                .filter(s -> !excl.contains(s.getTarget()))
                .limit(safeLimit)
                .map(RecommendationItem::fromItemSimilarity)
                .toList();
    }

    @Cacheable(value = "similarItemsContent", key = "#productId")
    public List<ItemSimilarityEntity> fetchTopBySource(Integer productId) {
        log.debug("CACHE MISS - Content similar for product {}", productId);
        return itemSimilarityRepository.findTopBySource(
                productId,
                RecommendationAlgorithm.CONTENT_TFIDF,
                PageRequest.of(0, FETCH_BUFFER));
    }
}