package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
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

    Page<ProductFullResponse> getProductsByCategoryId(Long categoryId, int page, int limit);

    ProductFullResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);
}
