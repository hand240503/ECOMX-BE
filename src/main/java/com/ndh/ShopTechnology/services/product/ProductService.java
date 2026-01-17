package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductResponse;

import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);

    List<ProductResponse> getAllProducts();

    Page<ProductResponse> getProducts(int page, int limit);

    List<ProductResponse> getFeaturedProducts(int limit);

    List<ProductResponse> getBestSellingProducts(int limit);

    ProductResponse getProductById(Long id);

    List<ProductResponse> getProductsByCategoryId(Long categoryId);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);
}
