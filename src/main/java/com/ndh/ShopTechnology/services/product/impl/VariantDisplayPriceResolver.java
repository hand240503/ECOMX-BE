package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.repository.ProductPriceChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Giá đơn vị hiển thị storefront: ưu tiên price change đang hiệu lực, fallback catalog {@link PriceEntity#getCurrentValue()}
 * (nhỏ nhất trong các dòng giá của biến thể).
 */
@Component
@RequiredArgsConstructor
public class VariantDisplayPriceResolver {

    private final ProductPriceChangeRepository productPriceChangeRepository;

    /**
     * @return map variantId → đơn giá hiển thị; chỉ chứa biến thể resolve được giá (kể cả 0 nếu catalog = 0)
     */
    public Map<Long, Double> resolveForProducts(Collection<ProductEntity> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        Date at = new Date();
        List<Long> variantIds = new ArrayList<>();
        for (ProductEntity p : products) {
            if (p.getVariants() == null) {
                continue;
            }
            for (ProductVariantEntity v : p.getVariants()) {
                if (v != null && v.getId() != null) {
                    variantIds.add(v.getId());
                }
            }
        }
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProductPriceChangeEntity> bestChangeByVariantId =
                effectiveActivePriceChangesByVariantId(variantIds, at);

        Map<Long, Double> out = new LinkedHashMap<>();
        for (ProductEntity p : products) {
            if (p.getVariants() == null) {
                continue;
            }
            for (ProductVariantEntity v : p.getVariants()) {
                if (v == null || v.getId() == null) {
                    continue;
                }
                Double unit = resolveUnitForVariant(v, bestChangeByVariantId.get(v.getId()));
                if (unit != null) {
                    out.put(v.getId(), unit);
                }
            }
        }
        return out;
    }

    /**
     * Dòng price change đang hiệu lực theo cùng quy tắc {@link #resolveForProducts} (startAt/id giảm dần).
     */
    public Map<Long, ProductPriceChangeEntity> effectiveActivePriceChangesByVariantId(
            Collection<Long> variantIds, Date at) {
        Date t = at != null ? at : new Date();
        if (variantIds == null || variantIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = variantIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ProductPriceChangeEntity> candidates =
                productPriceChangeRepository.findAllActiveCandidatesForVariantIdsAt(ids, t);
        return pickFirstPriceChangePerVariant(candidates);
    }

    private static Map<Long, ProductPriceChangeEntity> pickFirstPriceChangePerVariant(
            List<ProductPriceChangeEntity> orderedCandidates) {
        Map<Long, ProductPriceChangeEntity> m = new LinkedHashMap<>();
        if (orderedCandidates == null) {
            return m;
        }
        for (ProductPriceChangeEntity pc : orderedCandidates) {
            if (pc == null || pc.getProductVariant() == null || pc.getProductVariant().getId() == null) {
                continue;
            }
            Long vid = pc.getProductVariant().getId();
            m.putIfAbsent(vid, pc);
        }
        return m;
    }

    private static Double resolveUnitForVariant(ProductVariantEntity v, ProductPriceChangeEntity pc) {
        if (pc != null) {
            if (pc.getSalePrice() != null) {
                return pc.getSalePrice();
            }
            if (pc.getBasePrice() != null) {
                return pc.getBasePrice();
            }
        }
        return minCatalogCurrentValue(v);
    }

    private static Double minCatalogCurrentValue(ProductVariantEntity v) {
        if (v.getPrices() == null || v.getPrices().isEmpty()) {
            return null;
        }
        return v.getPrices().stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(PriceEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(PriceEntity::getCurrentValue)
                .orElse(null);
    }
}
