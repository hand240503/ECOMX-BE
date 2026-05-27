package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.request.product.UpsertProductPriceRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceResponse;

import java.util.List;

public interface ProductPriceService {

    List<ProductPriceResponse> list(Long productId);

    ProductPriceResponse create(Long productId, UpsertProductPriceRequest request);

    ProductPriceResponse update(Long productId, long priceId, UpsertProductPriceRequest request);

    void delete(Long productId, long priceId);
}
