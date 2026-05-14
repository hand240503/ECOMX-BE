package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductVariantPriceHydrator {

    private final PriceRepository priceRepository;

    /**
     * Loads catalog prices (+ unit) for variants and fills {@link ProductVariantEntity#getPrices()}.
     * Variant and prices cannot be join-fetched together with {@link ProductEntity#getVariants()}
     * in one query ({@code MultipleBagFetchException}).
     */
    public void attachPrices(Collection<ProductEntity> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> variantIds = new ArrayList<>();
        for (ProductEntity p : products) {
            if (p.getVariants() == null) {
                continue;
            }
            for (ProductVariantEntity v : p.getVariants()) {
                if (v.getId() != null) {
                    variantIds.add(v.getId());
                }
            }
        }
        if (variantIds.isEmpty()) {
            return;
        }
        List<PriceEntity> rows = priceRepository.findAllWithUnitByVariantIdIn(variantIds);
        Map<Long, List<PriceEntity>> byVariantId = new LinkedHashMap<>();
        for (PriceEntity pr : rows) {
            Long vid = pr.getVariant().getId();
            byVariantId.computeIfAbsent(vid, k -> new ArrayList<>()).add(pr);
        }
        for (ProductEntity p : products) {
            if (p.getVariants() == null) {
                continue;
            }
            for (ProductVariantEntity v : p.getVariants()) {
                if (v.getId() == null) {
                    continue;
                }
                List<PriceEntity> list = byVariantId.get(v.getId());
                v.getPrices().clear();
                if (list != null) {
                    v.getPrices().addAll(list);
                }
            }
        }
    }
}
