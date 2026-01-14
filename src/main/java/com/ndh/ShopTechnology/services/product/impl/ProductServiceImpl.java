package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductResponse;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.product.ProductService;
import com.ndh.ShopTechnology.repository.PriceRepository;
import com.ndh.ShopTechnology.repository.UnitRepository;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.dto.request.product.CreatePriceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final PriceRepository priceRepository;

    public ProductServiceImpl(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UnitRepository unitRepository,
            PriceRepository priceRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.priceRepository = priceRepository;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        // Verify category exists
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new NotFoundEntityException("Category not found with id: " + request.getCategoryId()));

        ProductEntity product = ProductEntity.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .status(request.getStatus())
                .description(request.getDescription())
                .status(request.getStatus())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .category(category)
                .build();

        // Handle prices
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
        return ProductResponse.fromEntity(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        List<ProductEntity> products = productRepository.findAll();
        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProducts(Long lastId, int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        List<ProductEntity> products;

        if (lastId == null) {
            // First page, query order by ID desc
            products = productRepository.findAll(
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
            if (products.size() > limit) {
                products = products.subList(0, limit);
            }
        } else {
            // Next pages
            products = productRepository.findByIdLessThanOrderByIdDesc(lastId, pageable);
        }

        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getFeaturedProducts(int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        List<ProductEntity> products = productRepository.findByIsFeaturedTrue(pageable);
        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getBestSellingProducts(int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        List<ProductEntity> products = productRepository.findTopNByOrderBySoldCountDesc(pageable);
        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse getProductById(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Override
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        // Verify category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundEntityException("Category not found with id: " + categoryId);
        }

        List<ProductEntity> products = productRepository.findByCategoryId(categoryId);
        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));

        // Update fields if provided
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

        // Handle category change
        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));
        productRepository.delete(product);
    }
}
