package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.search.ProductSearchResult;
import com.ndh.ShopTechnology.dto.response.product.ActivePromotionsResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductDetailResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductFullResponse createProduct(CreateProductRequest request);

    List<ProductFullResponse> getAllProducts();

    Page<ProductFullResponse> getProducts(int page, int limit);

    Page<ProductFullResponse> getAdminProducts(int page, int limit);

    List<ProductFullResponse> getFeaturedProducts(int limit, boolean all);

    List<ProductFullResponse> getHotSaleProducts(int limit, boolean all);

    List<ProductFullResponse> getBestSellingProducts(int limit);

    ProductFullResponse getProductById(Long id);

    List<ProductFullResponse> getProductsByIds(List<Long> productIds);

    ProductDetailResponse getProductDetail(Long id, Long userId, String sessionId, int recommendationLimit);

    Page<ProductFullResponse> getProductsByCategoryId(Long categoryId, int page, int limit);

    Page<ProductFullResponse> getAdminProductsByCategoryId(Long categoryId, int page, int limit);

    ProductSearchResult searchProducts(String query, int page, int limit);

    ProductFullResponse updateProduct(Long id, UpdateProductRequest request, List<MultipartFile> newImages);

    ProductFullResponse addVariantImages(
            Long productId,
            Long variantId,
            List<MultipartFile> newImages,
            Integer mainNewImageIndex);

    void deleteProduct(Long id);

    ActivePromotionsResponse getActivePromotions();
}
