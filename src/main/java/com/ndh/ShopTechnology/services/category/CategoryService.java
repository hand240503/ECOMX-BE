package com.ndh.ShopTechnology.services.category;

import com.ndh.ShopTechnology.dto.request.category.CreateCategoryRequest;
import com.ndh.ShopTechnology.dto.request.category.UpdateCategoryRequest;
import com.ndh.ShopTechnology.dto.response.category.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest req);
    List<CategoryResponse> getAllCategories();

    /**
     * Featured root categories for the search landing page (subset of roots, stable order by id).
     */
    List<CategoryResponse> getFeaturedCategories(int limit);

    List<CategoryResponse> getRootCategories(); // Get only parent categories
    List<CategoryResponse> getChildCategories(Long parentId); // Get children of a parent
    CategoryResponse getCategoryById(Long id);
    CategoryResponse updateCategory(Long id, UpdateCategoryRequest req);
    void deleteCategory(Long id);
}
