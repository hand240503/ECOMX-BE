package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.request.product.UpsertPriceChangeRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceChangeResponse;

import java.util.List;

public interface ProductPriceChangeService {

    List<ProductPriceChangeResponse> list(Long productId);

    ProductPriceChangeResponse create(Long productId, UpsertPriceChangeRequest request);

    ProductPriceChangeResponse update(Long productId, long priceChangeId, UpsertPriceChangeRequest request);

    void delete(Long productId, long priceChangeId);
}

