package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.entities.product.ProductEntity;

import java.util.Date;

/**
 * Resolve đơn giá hiệu lực của sản phẩm theo thời gian (price change).
 */
public interface ProductEffectivePriceService {

    /**
     * @param at thời điểm áp dụng giá (thường là thời điểm tạo đơn)
     */
    double resolveEffectiveUnitPrice(ProductEntity product, Date at);
}

