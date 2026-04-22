package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.search.ProductSearchResult;
import com.ndh.ShopTechnology.dto.response.product.ProductDetailResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;

import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    ProductFullResponse createProduct(CreateProductRequest request);

    List<ProductFullResponse> getAllProducts();

    Page<ProductFullResponse> getProducts(int page, int limit);

    List<ProductFullResponse> getFeaturedProducts(int limit);

    List<ProductFullResponse> getBestSellingProducts(int limit);

    ProductFullResponse getProductById(Long id);

    /**
     * Trả về {@link ProductFullResponse} cho đúng các id (giữ thứ tự FE gửi, bỏ trùng và id không có trong DB).
     */
    List<ProductFullResponse> getProductsByIds(List<Long> productIds);

    /**
     * PDP payload: full product row (prices, category, brand, ratings) plus similar-item recommendations.
     */
    ProductDetailResponse getProductDetail(Long id, Long userId, String sessionId, int recommendationLimit);

    Page<ProductFullResponse> getProductsByCategoryId(Long categoryId, int page, int limit);

    /**
     * Search products by keyword (name, description, tag). Empty or blank query returns an empty page.
     */
    ProductSearchResult searchProducts(String query, int page, int limit);

    ProductFullResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);
}
