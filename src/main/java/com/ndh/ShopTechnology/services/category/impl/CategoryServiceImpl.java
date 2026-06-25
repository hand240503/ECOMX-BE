package com.ndh.ShopTechnology.services.category.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.annotation.AdminAudit;
import com.ndh.ShopTechnology.aspect.SnapshotFetcherRegistry;
import com.ndh.ShopTechnology.constants.DocumentEntityType;
import com.ndh.ShopTechnology.dto.request.category.CreateCategoryRequest;
import com.ndh.ShopTechnology.dto.request.category.UpdateCategoryRequest;
import com.ndh.ShopTechnology.dto.response.category.CategoryBulkDeleteResponse;
import com.ndh.ShopTechnology.dto.response.category.CategoryResponse;
import com.ndh.ShopTechnology.dto.response.product.BrandSummaryResponse;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.DocumentRepository;
import com.ndh.ShopTechnology.services.category.CategoryService;
import com.ndh.ShopTechnology.services.user.UserService;
import jakarta.annotation.PostConstruct;
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
    private final com.ndh.ShopTechnology.repository.ProductRepository productRepository;
    private final UserService userService;
    private final SnapshotFetcherRegistry snapshotFetcherRegistry;
    private final ObjectMapper objectMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               DocumentRepository documentRepository,
                               BrandRepository brandRepository,
                               com.ndh.ShopTechnology.repository.ProductRepository productRepository,
                               UserService userService,
                               SnapshotFetcherRegistry snapshotFetcherRegistry,
                               ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.documentRepository = documentRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.userService = userService;
        this.snapshotFetcherRegistry = snapshotFetcherRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void registerSnapshotFetcher() {
        snapshotFetcherRegistry.register(AdminActivityLogEntity.ENTITY_CATEGORY, id ->
                categoryRepository.findById(id).map(e -> {
                    try {
                        return objectMapper.writeValueAsString(
                                CategoryResponse.fromEntitySimple(e, resolveThumbnail(e.getId())));
                    } catch (Exception ex) {
                        return null;
                    }
                }).orElse(null));
    }

    private String resolveThumbnail(Long categoryId) {
        return documentRepository
                .findMainByEntityIdAndEntityType(categoryId, DocumentEntityType.ID_DOCUMENT_ENTITY_CATEGORY)
                .map(d -> d.getFilePath())
                .orElse(null);
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_CATEGORY,
        action     = AdminActivityLogEntity.ACTION_CREATE,
        idArgIndex = -1
    )
    public CategoryResponse createCategory(CreateCategoryRequest req) {
        UserEntity currentUser = userService.getCurrentUser();

        if (categoryRepository.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Category code already exists: " + req.getCode());
        }

        CategoryEntity.CategoryEntityBuilder builder = CategoryEntity.builder()
                .code(req.getCode())
                .name(req.getName())
                .status(req.getStatus());

        if (req.getParentId() != null) {
            CategoryEntity parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Parent category not found with id: " + req.getParentId()));
            builder.parent(parent);
        }

        CategoryEntity ent = builder.build();
        ent = categoryRepository.save(ent);
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
    @AdminAudit(
        entityType            = AdminActivityLogEntity.ENTITY_CATEGORY,
        action                = AdminActivityLogEntity.ACTION_UPDATE,
        idArgIndex            = 0,
        captureSnapshotBefore = true
    )
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest req) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Category not found with id: " + id));

        if (req.getCode() != null && !req.getCode().equals(category.getCode())) {
            if (categoryRepository.existsByCode(req.getCode())) {
                throw new IllegalArgumentException("Category code already exists: " + req.getCode());
            }
            category.setCode(req.getCode());
        }
        if (req.getName() != null)   category.setName(req.getName());
        if (req.getStatus() != null) category.setStatus(req.getStatus());

        if (req.getParentId() != null) {
            if (req.getParentId().equals(id))
                throw new IllegalArgumentException("Category cannot be its own parent");
            CategoryEntity parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Parent category not found with id: " + req.getParentId()));
            category.setParent(parent);
        } else if (category.getParent() != null) {
            category.setParent(null);
        }

        category = categoryRepository.save(category);
        return CategoryResponse.fromEntity(category, resolveThumbnail(id));
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType            = AdminActivityLogEntity.ENTITY_CATEGORY,
        action                = AdminActivityLogEntity.ACTION_DELETE,
        idArgIndex            = 0,
        captureSnapshotBefore = true
    )
    public void deleteCategory(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Category not found with id: " + id));

        List<Long> ids = List.of(id);
        // Gỡ danh mục khỏi sản phẩm (set null) và đưa danh mục con lên gốc (parent null) trước khi xóa.
        productRepository.clearCategoryForCategoryIds(ids);
        categoryRepository.detachChildrenOfParents(ids);
        // EntityManager đã được clear (clearAutomatically) -> nạp lại để xóa an toàn (children đã rỗng).
        categoryRepository.findById(id).ifPresent(categoryRepository::delete);
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_CATEGORY,
        action     = AdminActivityLogEntity.ACTION_DELETE,
        idArgIndex = -1
    )
    public CategoryBulkDeleteResponse deleteCategories(List<Long> ids) {
        List<Long> requested = (ids == null) ? List.of() : ids.stream()
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("Danh sách danh mục cần xóa đang trống");
        }

        List<CategoryEntity> found = categoryRepository.findAllById(requested);
        List<Long> foundIds = found.stream().map(CategoryEntity::getId).collect(Collectors.toList());
        List<Long> notFound = requested.stream()
                .filter(rid -> !foundIds.contains(rid)).collect(Collectors.toList());

        int productsDetached = 0, childrenDetached = 0, deleted = 0;
        if (!foundIds.isEmpty()) {
            // Gỡ sản phẩm (category=null) và đưa danh mục con lên gốc (parent=null) cho toàn bộ danh mục cần xóa.
            productsDetached = productRepository.clearCategoryForCategoryIds(foundIds);
            childrenDetached = categoryRepository.detachChildrenOfParents(foundIds);
            // EntityManager đã clear -> nạp lại bản sạch rồi xóa (children đã rỗng nên không cascade nhầm).
            List<CategoryEntity> fresh = categoryRepository.findAllById(foundIds);
            categoryRepository.deleteAll(fresh);
            deleted = fresh.size();
        }

        return CategoryBulkDeleteResponse.builder()
                .requested(requested.size())
                .deleted(deleted)
                .productsDetached(productsDetached)
                .childrenDetached(childrenDetached)
                .notFoundIds(notFound)
                .build();
    }
}
