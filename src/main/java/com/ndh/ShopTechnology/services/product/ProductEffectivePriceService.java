package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;

import java.util.Date;

public interface ProductEffectivePriceService {

    double resolveEffectiveUnitPrice(ProductVariantEntity variant, Date at);
}
