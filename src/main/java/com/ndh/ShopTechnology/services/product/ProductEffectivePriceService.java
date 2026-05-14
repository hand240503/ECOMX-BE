package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;

import java.util.Date;

/**
 * Resolve đơn giá hiệu lực theo thời gian (price change) cho từng biến thể.
 */
public interface ProductEffectivePriceService {

    /**
     * @param at thời điểm áp dụng giá (thường là thời điểm tạo đơn)
     */
    double resolveEffectiveUnitPrice(ProductVariantEntity variant, Date at);
}
