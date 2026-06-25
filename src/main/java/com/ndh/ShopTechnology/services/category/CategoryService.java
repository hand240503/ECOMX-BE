package com.ndh.ShopTechnology.services.category;

import com.ndh.ShopTechnology.dto.request.category.CreateCategoryRequest;
import com.ndh.ShopTechnology.dto.request.category.UpdateCategoryRequest;
import com.ndh.ShopTechnology.dto.response.category.CategoryBulkDeleteResponse;
import com.ndh.ShopTechnology.dto.response.category.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest req);
    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getFeaturedCategories(int limit);

    List<CategoryResponse> getRootCategories();
    List<CategoryResponse> getChildCategories(Long parentId);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse updateCategory(Long id, UpdateCategoryRequest req);
    void deleteCategory(Long id);

    /** Xóa hàng loạt danh mục: sản phẩm thuộc danh mục được set null, danh mục con đưa lên gốc. */
    CategoryBulkDeleteResponse deleteCategories(List<Long> ids);
}
