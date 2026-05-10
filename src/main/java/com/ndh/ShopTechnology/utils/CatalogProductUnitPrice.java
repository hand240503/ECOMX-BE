package com.ndh.ShopTechnology.utils;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import org.springframework.http.HttpStatus;

import java.util.Comparator;
import java.util.Objects;

/**
 * Đơn giá catalog mặc định (bản ghi {@link PriceEntity} có id nhỏ nhất) — dùng khi không có bậc mix-and-match.
 */
public final class CatalogProductUnitPrice {

    private CatalogProductUnitPrice() {
    }

    public static double resolve(ProductEntity product) {
        if (product.getPrices() == null || product.getPrices().isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Product has no price: " + product.getId());
        }
        return product.getPrices().stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(com.ndh.ShopTechnology.entities.product.PriceEntity::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .map(com.ndh.ShopTechnology.entities.product.PriceEntity::getCurrentValue)
                .orElseThrow(() -> new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Product has no price: " + product.getId()));
    }
}
