package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.repository.ProductPriceChangeRepository;
import com.ndh.ShopTechnology.services.product.ProductEffectivePriceService;
import com.ndh.ShopTechnology.utils.CatalogProductUnitPrice;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ProductEffectivePriceServiceImpl implements ProductEffectivePriceService {

    private final ProductPriceChangeRepository priceChangeRepository;

    public ProductEffectivePriceServiceImpl(ProductPriceChangeRepository priceChangeRepository) {
        this.priceChangeRepository = priceChangeRepository;
    }

    @Override
    public double resolveEffectiveUnitPrice(ProductEntity product, Date at) {
        Date t = at != null ? at : new Date();
        if (product == null || product.getId() == null) {
            return 0.0;
        }
        ProductPriceChangeEntity pc = priceChangeRepository.findEffectiveForProductAt(product.getId(), t).orElse(null);
        if (pc == null) {
            return CatalogProductUnitPrice.resolve(product);
        }
        Double sale = pc.getSalePrice();
        Double base = pc.getBasePrice();
        if (sale != null) {
            return sale;
        }
        if (base != null) {
            return base;
        }
        return CatalogProductUnitPrice.resolve(product);
    }
}

