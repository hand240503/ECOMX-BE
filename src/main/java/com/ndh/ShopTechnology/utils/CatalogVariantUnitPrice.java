package com.ndh.ShopTechnology.utils;

import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import org.springframework.http.HttpStatus;

import java.util.Comparator;
import java.util.Objects;

/**
 * Đơn giá catalog mặc định của một biến thể (bản ghi {@link PriceEntity} có id nhỏ nhất).
 */
public final class CatalogVariantUnitPrice {

    private CatalogVariantUnitPrice() {
    }

    public static double resolve(ProductVariantEntity variant) {
        if (variant == null || variant.getId() == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Variant is required");
        }
        if (variant.getPrices() == null || variant.getPrices().isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Variant has no price: " + variant.getId());
        }
        return variant.getPrices().stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(PriceEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(PriceEntity::getCurrentValue)
                .orElseThrow(() -> new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Variant has no price: " + variant.getId()));
    }
}
