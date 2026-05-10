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

    /** Danh sách admin: phân trang, sắp xếp {@code id} tăng dần. */
    Page<ProductFullResponse> getAdminProducts(int page, int limit);

    /**
     * @param limit khi {@code all} là {@code false}: số bản ghi tối đa (mặc định controller 10);
     *              service chuẩn hoá tối thiểu 1 và áp trần an toàn.
     * @param all   {@code true}: trả về mọi sản phẩm thỏa điều kiện, sắp xếp {@code id} giảm dần; {@code limit} bị bỏ qua.
     */
    List<ProductFullResponse> getFeaturedProducts(int limit, boolean all);

    /**
     * Sản phẩm {@code hot_sale = true}, cùng định dạng list với {@link #getFeaturedProducts(int, boolean)}.
     */
    List<ProductFullResponse> getHotSaleProducts(int limit, boolean all);

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

    /** Admin: sản phẩm theo category, {@code id} tăng dần. */
    Page<ProductFullResponse> getAdminProductsByCategoryId(Long categoryId, int page, int limit);

    /**
     * Search products by keyword (name, description, {@code l_description}, tag). Empty or blank query returns an empty page.
     */
    ProductSearchResult searchProducts(String query, int page, int limit);

    ProductFullResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);
}
