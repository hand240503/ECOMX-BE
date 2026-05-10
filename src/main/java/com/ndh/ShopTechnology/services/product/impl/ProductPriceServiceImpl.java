package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.request.product.UpsertProductPriceRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceResponse;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.PriceRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.UnitRepository;
import com.ndh.ShopTechnology.services.product.ProductPriceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductPriceServiceImpl implements ProductPriceService {

    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final UnitRepository unitRepository;

    public ProductPriceServiceImpl(ProductRepository productRepository,
            PriceRepository priceRepository,
            UnitRepository unitRepository) {
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPriceResponse> list(Long productId) {
        ensureProduct(productId);
        return priceRepository.findByProduct_IdOrderByIdAsc(productId).stream()
                .map(ProductPriceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductPriceResponse create(Long productId, UpsertProductPriceRequest request) {
        ProductEntity product = ensureProduct(productId);
        if (request.getUnitId() == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "unit_id is required");
        }
        UnitEntity unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new NotFoundEntityException("Unit not found with id: " + request.getUnitId()));

        PriceEntity entity = PriceEntity.builder()
                .product(product)
                .unit(unit)
                .currentValue(request.getCurrentValue())
                .oldValue(request.getOldValue() != null ? request.getOldValue() : 0.0)
                .build();
        return ProductPriceResponse.fromEntity(priceRepository.save(entity));
    }

    @Override
    @Transactional
    public ProductPriceResponse update(Long productId, long priceId, UpsertProductPriceRequest request) {
        ensureProduct(productId);
        PriceEntity entity = priceRepository.findById(priceId)
                .orElseThrow(() -> new NotFoundEntityException("Price not found with id: " + priceId));
        if (entity.getProduct() == null || !productId.equals(entity.getProduct().getId())) {
            throw new CustomApiException(HttpStatus.NOT_FOUND, "Price not found for product: " + productId);
        }
        if (request.getUnitId() != null
                && (entity.getUnit() == null || !request.getUnitId().equals(entity.getUnit().getId()))) {
            UnitEntity unit = unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Unit not found with id: " + request.getUnitId()));
            entity.setUnit(unit);
        }
        entity.setCurrentValue(request.getCurrentValue());
        if (request.getOldValue() != null) {
            entity.setOldValue(request.getOldValue());
        }
        return ProductPriceResponse.fromEntity(priceRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long productId, long priceId) {
        ensureProduct(productId);
        PriceEntity entity = priceRepository.findById(priceId)
                .orElseThrow(() -> new NotFoundEntityException("Price not found with id: " + priceId));
        if (entity.getProduct() == null || !productId.equals(entity.getProduct().getId())) {
            throw new CustomApiException(HttpStatus.NOT_FOUND, "Price not found for product: " + productId);
        }
        priceRepository.delete(entity);
    }

    private ProductEntity ensureProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + productId));
    }
}
