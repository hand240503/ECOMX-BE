package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.request.product.UpsertPriceChangeRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceChangeResponse;

import java.util.List;

public interface ProductPriceChangeService {

    List<ProductPriceChangeResponse> list(Long productId, Long variantId);

    /** Tất cả chương trình đổi giá (mọi sản phẩm/biến thể) cho trang tổng quan. */
    List<ProductPriceChangeResponse> listAll();

    ProductPriceChangeResponse create(Long productId, Long variantId, UpsertPriceChangeRequest request);

    ProductPriceChangeResponse update(Long productId, Long variantId, long priceChangeId,
            UpsertPriceChangeRequest request);

    void delete(Long productId, Long variantId, long priceChangeId);

    boolean incrementSoldQuantity(Long priceChangeId, int qty);

    boolean isWithinPerCustomerLimit(Long priceChangeId, Long userId, int qty);
}
