package com.ndh.ShopTechnology.services.category.impl;

import com.ndh.ShopTechnology.constants.DocumentEntityType;
import com.ndh.ShopTechnology.dto.request.category.CreateCategoryRequest;
import com.ndh.ShopTechnology.dto.request.category.UpdateCategoryRequest;
import com.ndh.ShopTechnology.dto.response.category.CategoryResponse;
import com.ndh.ShopTechnology.dto.response.product.BrandSummaryResponse;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.DocumentRepository;
import com.ndh.ShopTechnology.services.category.CategoryService;
import com.ndh.ShopTechnology.services.user.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;
    private final BrandRepository brandRepository;
    private final UserService userService;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               DocumentRepository documentRepository,
                               BrandRepository brandRepository,
                               UserService userService) {
        this.categoryRepository = categoryRepository;
        this.documentRepository = documentRepository;
        this.brandRepository = brandRepository;
        this.userService = userService;
    }

    /** Lấy URL ảnh đại diện (is_main=true) của một category; trả về {@code null} nếu chưa có. */
    private String resolveThumbnail(Long categoryId) {
        return documentRepository
                .findMainByEntityIdAndEntityType(categoryId, DocumentEntityType.ID_DOCUMENT_ENTITY_CATEGORY)
                .map(d -> d.getFilePath())
                .orElse(null);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest req) {
        UserEntity currentUser = userService.getCurrentUser();

        // Check if code already exists
        if (categoryRepository.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Category code already exists: " + req.getCode());
        }

        CategoryEntity.CategoryEntityBuilder builder = CategoryEntity.builder()
                .code(req.getCode())
                .name(req.getName())
                .status(req.getStatus());

        // Set parent if provided
        if (req.getParentId() != null) {
            CategoryEntity parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Parent category not found with id: " + req.getParentId()));
            builder.parent(parent);
        }

        CategoryEntity ent = builder.build();
        ent = categoryRepository.save(ent);
        // Ảnh chưa upload ngay khi tạo mới → thumbnailUrl = null
        return CategoryResponse.fromEntity(ent, null);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<CategoryEntity> categories = categoryRepository.findAll();
        return categories.stream()
                .map(c -> CategoryResponse.fromEntitySimple(c, resolveThumbnail(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getFeaturedCategories(int limit) {
        int lim = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(0, lim, Sort.by(Sort.Direction.ASC, "id"));
        return categoryRepository.findByParentIsNull(pageable).getContent().stream()
                .map(c -> CategoryResponse.fromEntitySimple(c, resolveThumbnail(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getRootCategories() {
        List<CategoryEntity> rootCategories = categoryRepository.findByParentIsNull();
        return rootCategories.stream()
                .map(c -> {
                    List<BrandSummaryResponse> brands = brandRepository
                            .findDistinctBrandsByCategoryId(c.getId())
                            .stream()
                            .map(b -> {
                                String logoUrl = documentRepository
                                        .findMainByEntityIdAndEntityType(
                                                b.getId(),
                                                DocumentEntityType.ID_DOCUMENT_ENTITY_BRAND)
                                        .map(d -> d.getFilePath())
                                        .orElse(null);
                                return BrandSummaryResponse.fromEntity(b, logoUrl);
                            })
                            .collect(Collectors.toList());
                    CategoryResponse resp = CategoryResponse.fromEntity(c, resolveThumbnail(c.getId()));
                    resp.setBrands(brands.isEmpty() ? null : brands);
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getChildCategories(Long parentId) {
        categoryRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundEntityException("Parent category not found with id: " + parentId));

        List<CategoryEntity> children = categoryRepository.findByParentId(parentId);
        return children.stream()
                .map(c -> CategoryResponse.fromEntitySimple(c, resolveThumbnail(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Category not found with id: " + id));
        return CategoryResponse.fromEntity(category, resolveThumbnail(id));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest req) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Category not found with id: " + id));

        // Update fields if provided
        if (req.getCode() != null && !req.getCode().equals(category.getCode())) {
            // Check if new code already exists
            if (categoryRepository.existsByCode(req.getCode())) {
                throw new IllegalArgumentException("Category code already exists: " + req.getCode());
            }
            category.setCode(req.getCode());
        }

        if (req.getName() != null) {
            category.setName(req.getName());
        }

        if (req.getStatus() != null) {
            category.setStatus(req.getStatus());
        }

        // Handle parent change
        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            CategoryEntity parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Parent category not found with id: " + req.getParentId()));
            category.setParent(parent);
        } else if (req.getParentId() == null && category.getParent() != null) {
            // Setting parentId to null means making it a root category
            category.setParent(null);
        }

        category = categoryRepository.save(category);
        return CategoryResponse.fromEntity(category, resolveThumbnail(id));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Category not found with id: " + id));

        // Check if category has children
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete category with children. Please delete or move children first.");
        }

        // Check if category has products
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete category with products. Please remove products first.");
        }

        categoryRepository.delete(category);
    }
}
