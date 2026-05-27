package com.ndh.ShopTechnology.utils;

import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public final class ProductCatalogListing {

    private ProductCatalogListing() {
    }

    public static ProductVariantEntity pickCheapestActiveVariant(ProductEntity product) {
        return pickCheapestActiveVariant(product, null);
    }

    public static ProductVariantEntity pickCheapestActiveVariant(
            ProductEntity product, Map<Long, Double> displayUnitPriceByVariantId) {
        if (product == null || product.getVariants() == null || product.getVariants().isEmpty()) {
            return null;
        }
        return product.getVariants().stream()
                .filter(Objects::nonNull)
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .min(Comparator.comparingDouble(v -> unitForListing(v, displayUnitPriceByVariantId)))
                .orElse(null);
    }

    private static double unitForListing(ProductVariantEntity v, Map<Long, Double> displayUnitPriceByVariantId) {
        if (displayUnitPriceByVariantId != null && v.getId() != null) {
            Double d = displayUnitPriceByVariantId.get(v.getId());
            if (d != null) {
                return d;
            }
        }
        return minCatalogUnit(v);
    }

    private static double minCatalogUnit(ProductVariantEntity v) {
        if (v.getPrices() == null || v.getPrices().isEmpty()) {
            return Double.MAX_VALUE;
        }
        return v.getPrices().stream()
                .filter(Objects::nonNull)
                .mapToDouble(PriceEntity::getCurrentValue)
                .filter(Objects::nonNull)
                .min()
                .orElse(Double.MAX_VALUE);
    }
}
