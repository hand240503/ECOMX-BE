package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.UnitRepository;
import com.ndh.ShopTechnology.repository.UserRatingRepository;
import com.ndh.ShopTechnology.repository.projection.ProductRatingAggregate;
import com.ndh.ShopTechnology.services.product.ProductService;
import com.ndh.ShopTechnology.dto.request.product.CreatePriceRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final UserRatingRepository userRatingRepository;

    public ProductServiceImpl(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UnitRepository unitRepository,
            UserRatingRepository userRatingRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.userRatingRepository = userRatingRepository;
    }

    @Override
    @Transactional
    public ProductFullResponse createProduct(CreateProductRequest request) {
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new NotFoundEntityException("Category not found with id: " + request.getCategoryId()));

        ProductEntity product = ProductEntity.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .status(request.getStatus())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .category(category)
                .build();

        if (request.getPrices() != null && !request.getPrices().isEmpty()) {
            for (CreatePriceRequest priceRequest : request.getPrices()) {
                UnitEntity unit = unitRepository.findById(priceRequest.getUnitId())
                        .orElseThrow(() -> new NotFoundEntityException(
                                "Unit not found with id: " + priceRequest.getUnitId()));

                PriceEntity price = PriceEntity.builder()
                        .unit(unit)
                        .product(product)
                        .currentValue(priceRequest.getCurrentValue())
                        .oldValue(priceRequest.getOldValue() != null ? priceRequest.getOldValue() : 0.0)
                        .build();

                product.getPrices().add(price);
            }
        }

        product = productRepository.save(product);
        ProductEntity full = productRepository.findWithFullRelationsById(product.getId())
                .orElse(product);
        return toFullWithRatings(full);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getAllProducts() {
        List<ProductEntity> products = productRepository.findAllWithListRelations();
        return mapWithRatings(products);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductFullResponse> getProducts(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        Page<ProductEntity> productPage = productRepository.findPageWithListRelations(pageable);
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                productPage.getContent().stream().map(ProductEntity::getId).toList());
        return productPage.map(p -> toFull(p, ratings));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getFeaturedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        List<ProductEntity> products = productRepository.findByIsFeaturedTrue(pageable);
        return mapWithRatings(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getBestSellingProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ProductEntity> products = productRepository.findTopNByOrderBySoldCountDesc(pageable);
        return mapWithRatings(products);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductFullResponse getProductById(Long id) {
        ProductEntity product = productRepository.findWithFullRelationsById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));
        return toFullWithRatings(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductFullResponse> getProductsByCategoryId(Long categoryId, int page, int limit) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundEntityException("Category not found with id: " + categoryId);
        }
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        boolean isParentCategory = categoryRepository.existsByParent_Id(categoryId);
        Page<ProductEntity> productPage = isParentCategory
                ? productRepository.findPageByDirectChildCategoriesOf(categoryId, pageable)
                : productRepository.findPageByCategoryId(categoryId, pageable);
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                productPage.getContent().stream().map(ProductEntity::getId).toList());
        return productPage.map(p -> toFull(p, ratings));
    }

    @Override
    @Transactional
    public ProductFullResponse updateProduct(Long id, UpdateProductRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));

        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        product = productRepository.save(product);
        ProductEntity full = productRepository.findWithFullRelationsById(product.getId())
                .orElse(product);
        return toFullWithRatings(full);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    private List<ProductFullResponse> mapWithRatings(List<ProductEntity> products) {
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                products.stream().map(ProductEntity::getId).toList());
        return products.stream().map(p -> toFull(p, ratings)).collect(Collectors.toList());
    }

    private Map<Long, ProductRatingAggregate> ratingMapForIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userRatingRepository.aggregateByProductIdIn(ids).stream()
                .collect(Collectors.toMap(ProductRatingAggregate::getProductId, Function.identity()));
    }

    private ProductFullResponse toFullWithRatings(ProductEntity p) {
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(List.of(p.getId()));
        return toFull(p, ratings);
    }

    private static ProductFullResponse toFull(ProductEntity p, Map<Long, ProductRatingAggregate> ratings) {
        ProductRatingAggregate a = ratings.get(p.getId());
        Double avg = a != null ? a.getAverageRating() : null;
        Long cnt = a != null ? a.getRatingCount() : null;
        return ProductFullResponse.fromEntity(p, null, null, avg, cnt);
    }
}
