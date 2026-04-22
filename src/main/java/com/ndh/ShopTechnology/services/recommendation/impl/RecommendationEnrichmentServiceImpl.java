package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.UserRatingRepository;
import com.ndh.ShopTechnology.repository.projection.ProductRatingAggregate;
import com.ndh.ShopTechnology.services.recommendation.RecommendationEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationEnrichmentServiceImpl
        implements RecommendationEnrichmentService {

    private final ProductRepository productRepository;
    private final UserRatingRepository userRatingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> enrich(List<RecommendationItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<Long> ids = items.stream()
                .map(RecommendationItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ProductEntity> productMap = productRepository.findAllWithFullRelationsByIdIn(ids)
                .stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        productMap.values().forEach(p -> Hibernate.initialize(p.getPolicies()));

        Map<Long, ProductRatingAggregate> ratingMap = userRatingRepository.aggregateByProductIdIn(ids)
                .stream()
                .collect(Collectors.toMap(ProductRatingAggregate::getProductId, Function.identity()));

        return items.stream()
                .map(it -> {
                    ProductEntity p = productMap.get(it.getProductId());
                    if (p == null) {
                        return null;
                    }
                    ProductRatingAggregate agg = ratingMap.get(it.getProductId());
                    Double avg = agg != null ? agg.getAverageRating() : null;
                    Long cnt = agg != null ? agg.getRatingCount() : null;
                    return ProductFullResponse.fromEntity(p, it.getScore(), it.getSource(), avg, cnt);
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
