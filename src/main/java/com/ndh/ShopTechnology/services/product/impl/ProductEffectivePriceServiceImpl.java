package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.repository.ProductPriceChangeRepository;
import com.ndh.ShopTechnology.services.product.ProductEffectivePriceService;
import com.ndh.ShopTechnology.utils.CatalogVariantUnitPrice;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ProductEffectivePriceServiceImpl implements ProductEffectivePriceService {

    private final ProductPriceChangeRepository priceChangeRepository;

    public ProductEffectivePriceServiceImpl(ProductPriceChangeRepository priceChangeRepository) {
        this.priceChangeRepository = priceChangeRepository;
    }

    @Override
    public double resolveEffectiveUnitPrice(ProductVariantEntity variant, Date at) {
        Date t = at != null ? at : new Date();
        if (variant == null || variant.getId() == null) {
            return 0.0;
        }
        ProductPriceChangeEntity pc = priceChangeRepository.findEffectiveForVariantAt(variant.getId(), t)
                .orElse(null);
        if (pc == null) {
            return CatalogVariantUnitPrice.resolve(variant);
        }
        Double sale = pc.getSalePrice();
        Double base = pc.getBasePrice();
        if (sale != null) {
            return sale;
        }
        if (base != null) {
            return base;
        }
        return CatalogVariantUnitPrice.resolve(variant);
    }
}
