package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.request.product.UpsertPriceChangeRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceChangeResponse;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.ProductPriceChangeRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.product.ProductPriceChangeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductPriceChangeServiceImpl implements ProductPriceChangeService {

    private final ProductRepository productRepository;
    private final ProductPriceChangeRepository priceChangeRepository;

    public ProductPriceChangeServiceImpl(
            ProductRepository productRepository,
            ProductPriceChangeRepository priceChangeRepository) {
        this.productRepository = productRepository;
        this.priceChangeRepository = priceChangeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPriceChangeResponse> list(Long productId) {
        ensureProduct(productId);
        return priceChangeRepository.findByProduct_IdOrderByStartAtDesc(productId).stream()
                .map(ProductPriceChangeServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductPriceChangeResponse create(Long productId, UpsertPriceChangeRequest request) {
        ProductEntity product = ensureProduct(productId);
        validateWindow(request.getStartAt(), request.getEndAt());
        ProductPriceChangeEntity e = ProductPriceChangeEntity.builder()
                .product(product)
                .basePrice(request.getBasePrice())
                .salePrice(request.getSalePrice())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        e = priceChangeRepository.save(e);
        return toResponse(e);
    }

    @Override
    @Transactional
    public ProductPriceChangeResponse update(Long productId, long priceChangeId, UpsertPriceChangeRequest request) {
        ensureProduct(productId);
        ProductPriceChangeEntity e = priceChangeRepository.findById(priceChangeId)
                .orElseThrow(() -> new NotFoundEntityException("PriceChange not found: " + priceChangeId));
        if (!e.getProduct().getId().equals(productId)) {
            throw new CustomApiException(HttpStatus.NOT_FOUND, "PriceChange not found for product: " + productId);
        }
        validateWindow(request.getStartAt(), request.getEndAt());
        e.setBasePrice(request.getBasePrice());
        e.setSalePrice(request.getSalePrice());
        e.setStartAt(request.getStartAt());
        e.setEndAt(request.getEndAt());
        if (request.getEnabled() != null) {
            e.setEnabled(request.getEnabled());
        }
        e = priceChangeRepository.save(e);
        return toResponse(e);
    }

    @Override
    @Transactional
    public void delete(Long productId, long priceChangeId) {
        ensureProduct(productId);
        ProductPriceChangeEntity e = priceChangeRepository.findById(priceChangeId)
                .orElseThrow(() -> new NotFoundEntityException("PriceChange not found: " + priceChangeId));
        if (!e.getProduct().getId().equals(productId)) {
            throw new CustomApiException(HttpStatus.NOT_FOUND, "PriceChange not found for product: " + productId);
        }
        priceChangeRepository.delete(e);
    }

    private ProductEntity ensureProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + productId));
    }

    private static void validateWindow(Date startAt, Date endAt) {
        if (startAt == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "startAt is required");
        }
        if (endAt != null && endAt.before(startAt)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "endAt must be >= startAt");
        }
    }

    private static ProductPriceChangeResponse toResponse(ProductPriceChangeEntity e) {
        return ProductPriceChangeResponse.builder()
                .id(e.getId())
                .productId(e.getProduct().getId())
                .basePrice(e.getBasePrice())
                .salePrice(e.getSalePrice())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .enabled(e.getEnabled())
                .build();
    }
}

