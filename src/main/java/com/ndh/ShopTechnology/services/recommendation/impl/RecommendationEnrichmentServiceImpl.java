package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.UserRatingRepository;
import com.ndh.ShopTechnology.repository.projection.ProductRatingAggregate;
import com.ndh.ShopTechnology.services.product.impl.ProductImageAttachService;
import com.ndh.ShopTechnology.services.product.impl.ProductPricingProgramsAttachService;
import com.ndh.ShopTechnology.services.product.impl.ProductVariantPriceHydrator;
import com.ndh.ShopTechnology.services.product.impl.VariantDisplayPriceResolver;
import com.ndh.ShopTechnology.services.recommendation.RecommendationEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final ProductImageAttachService productImageAttachService;
    private final ProductPricingProgramsAttachService productPricingProgramsAttachService;
    private final ProductVariantPriceHydrator productVariantPriceHydrator;
    private final VariantDisplayPriceResolver variantDisplayPriceResolver;

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

        List<ProductEntity> loaded = productRepository.findAllWithFullRelationsByIdIn(ids);
        productVariantPriceHydrator.attachPrices(loaded);
        Map<Long, ProductEntity> productMap = loaded.stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        productMap.values().forEach(p -> Hibernate.initialize(p.getPolicies()));

        Map<Long, Double> displayUnitPrices =
                variantDisplayPriceResolver.resolveForProducts(new ArrayList<>(productMap.values()));

        Map<Long, ProductRatingAggregate> ratingMap = userRatingRepository.aggregateByProductIdIn(ids)
                .stream()
                .collect(Collectors.toMap(ProductRatingAggregate::getProductId, Function.identity()));

        List<ProductFullResponse> enrichedList = items.stream()
                .map(it -> {
                    ProductEntity p = productMap.get(it.getProductId());
                    if (p == null) {
                        return null;
                    }
                    ProductRatingAggregate agg = ratingMap.get(it.getProductId());
                    Double avg = agg != null ? agg.getAverageRating() : null;
                    Long cnt = agg != null ? agg.getRatingCount() : null;
                    return ProductFullResponse.fromEntity(
                            p, it.getScore(), it.getSource(), avg, cnt, displayUnitPrices);
                })
                .filter(Objects::nonNull)
                .toList();
        productImageAttachService.attach(enrichedList);
        productPricingProgramsAttachService.attach(enrichedList);
        return enrichedList;
    }
}
