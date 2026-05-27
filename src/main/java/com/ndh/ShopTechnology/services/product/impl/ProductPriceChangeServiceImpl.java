package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.request.product.UpsertPriceChangeRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceChangeResponse;
import com.ndh.ShopTechnology.constants.MessageConstant;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.ProductPriceChangeRepository;
import com.ndh.ShopTechnology.repository.ProductPriceChangeUsageRepository;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.services.product.ProductPriceChangeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductPriceChangeServiceImpl implements ProductPriceChangeService {

    private final ProductVariantRepository variantRepository;
    private final ProductPriceChangeRepository priceChangeRepository;
    private final ProductPriceChangeUsageRepository usageRepository;

    public ProductPriceChangeServiceImpl(
            ProductVariantRepository variantRepository,
            ProductPriceChangeRepository priceChangeRepository,
            ProductPriceChangeUsageRepository usageRepository) {
        this.variantRepository = variantRepository;
        this.priceChangeRepository = priceChangeRepository;
        this.usageRepository = usageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPriceChangeResponse> list(Long productId, Long variantId) {
        ProductVariantEntity v = ensureVariant(productId, variantId);
        return priceChangeRepository.findByProductVariant_IdOrderByStartAtDesc(v.getId()).stream()
                .map(ProductPriceChangeServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductPriceChangeResponse create(Long productId, Long variantId, UpsertPriceChangeRequest request) {
        ProductVariantEntity v = ensureVariant(productId, variantId);
        validateWindow(request.getStartAt(), request.getEndAt());
        ProductPriceChangeEntity e = ProductPriceChangeEntity.builder()
                .productVariant(v)
                .productId(productId)
                .basePrice(request.getBasePrice())
                .salePrice(request.getSalePrice())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .quantityLimit(request.getQuantityLimit())
                .soldQuantity(0)
                .maxPerCustomer(request.getMaxPerCustomer())
                .requiredPaymentMethodCode(normalizeCode(request.getRequiredPaymentMethodCode()))
                .build();
        e = priceChangeRepository.save(e);
        return toResponse(e);
    }

    @Override
    @Transactional
    public ProductPriceChangeResponse update(Long productId, Long variantId, long priceChangeId,
            UpsertPriceChangeRequest request) {
        ensureVariant(productId, variantId);
        ProductPriceChangeEntity e = priceChangeRepository.findById(priceChangeId)
                .orElseThrow(() -> new NotFoundEntityException("PriceChange not found: " + priceChangeId));
        if (e.getProductVariant() == null || !e.getProductVariant().getId().equals(variantId)) {
            throw new CustomApiException(HttpStatus.NOT_FOUND,
                    "PriceChange not found for variant: " + variantId);
        }
        validateWindow(request.getStartAt(), request.getEndAt());
        e.setProductId(productId);
        e.setBasePrice(request.getBasePrice());
        e.setSalePrice(request.getSalePrice());
        e.setStartAt(request.getStartAt());
        e.setEndAt(request.getEndAt());
        if (request.getEnabled() != null) {
            e.setEnabled(request.getEnabled());
        }
        if (request.getQuantityLimit() != null) {
            e.setQuantityLimit(request.getQuantityLimit());
        }
        if (request.getMaxPerCustomer() != null) {
            e.setMaxPerCustomer(request.getMaxPerCustomer());
        }
        if (request.getRequiredPaymentMethodCode() != null) {
            e.setRequiredPaymentMethodCode(normalizeCode(request.getRequiredPaymentMethodCode()));
        }
        e = priceChangeRepository.save(e);
        return toResponse(e);
    }

    @Override
    @Transactional
    public void delete(Long productId, Long variantId, long priceChangeId) {
        ensureVariant(productId, variantId);
        ProductPriceChangeEntity e = priceChangeRepository.findById(priceChangeId)
                .orElseThrow(() -> new NotFoundEntityException("PriceChange not found: " + priceChangeId));
        if (e.getProductVariant() == null || !e.getProductVariant().getId().equals(variantId)) {
            throw new CustomApiException(HttpStatus.NOT_FOUND,
                    "PriceChange not found for variant: " + variantId);
        }
        assertDeletablePriceChange(e);
        priceChangeRepository.delete(e);
    }

    @Override
    @Transactional
    public boolean incrementSoldQuantity(Long priceChangeId, int qty) {
        if (priceChangeId == null || qty <= 0) return true;
        int affected = priceChangeRepository.incrementSoldQuantity(priceChangeId, qty);
        return affected > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWithinPerCustomerLimit(Long priceChangeId, Long userId, int qty) {
        if (priceChangeId == null || userId == null) return true;
        ProductPriceChangeEntity pc = priceChangeRepository.findById(priceChangeId).orElse(null);
        if (pc == null || pc.getMaxPerCustomer() == null) return true;
        int alreadyUsed = usageRepository.sumQuantityByPriceChangeIdAndUserId(priceChangeId, userId);
        return alreadyUsed + qty <= pc.getMaxPerCustomer();
    }

    private ProductVariantEntity ensureVariant(Long productId, Long variantId) {
        ProductVariantEntity v = variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundEntityException("Product variant not found with id: " + variantId));
        if (v.getProduct() == null || !productId.equals(v.getProduct().getId())) {
            throw new CustomApiException(HttpStatus.NOT_FOUND,
                    "Variant " + variantId + " does not belong to product " + productId);
        }
        return v;
    }

    private static void assertDeletablePriceChange(ProductPriceChangeEntity e) {
        Date now = new Date();
        if (e.getStartAt() != null && !e.getStartAt().after(now)) {
            throw new CustomApiException(HttpStatus.CONFLICT, MessageConstant.PRICE_CHANGE_CANNOT_DELETE_AFTER_START);
        }
    }

    private static void validateWindow(Date startAt, Date endAt) {
        if (startAt == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "startAt is required");
        }
        if (endAt != null && endAt.before(startAt)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "endAt must be >= startAt");
        }
    }

    private static String normalizeCode(String code) {
        if (code == null) return null;
        String t = code.trim().toUpperCase();
        return t.isEmpty() ? null : t;
    }

    static ProductPriceChangeResponse toResponse(ProductPriceChangeEntity e) {
        Integer ql = e.getQuantityLimit();
        Integer sq = e.getSoldQuantity() != null ? e.getSoldQuantity() : 0;
        Integer remaining = ql != null ? Math.max(0, ql - sq) : null;
        return ProductPriceChangeResponse.builder()
                .id(e.getId())
                .productVariantId(e.getProductVariant() != null ? e.getProductVariant().getId() : null)
                .basePrice(e.getBasePrice())
                .salePrice(e.getSalePrice())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .enabled(e.getEnabled())
                .quantityLimit(ql)
                .soldQuantity(sq)
                .remainingQuantity(remaining)
                .maxPerCustomer(e.getMaxPerCustomer())
                .requiredPaymentMethodCode(e.getRequiredPaymentMethodCode())
                .build();
    }
}
