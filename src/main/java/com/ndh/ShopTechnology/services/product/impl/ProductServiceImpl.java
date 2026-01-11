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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, 
                              CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        // Verify category exists
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundEntityException("Category not found with id: " + request.getCategoryId()));

        ProductEntity product = ProductEntity.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .status(request.getStatus())
                .category(category)
                .build();

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

        // Handle category change
        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundEntityException("Category not found with id: " + request.getCategoryId()));
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
