package com.ndh.ShopTechnology.services.promotion.impl;

import com.ndh.ShopTechnology.annotation.AdminAudit;
import com.ndh.ShopTechnology.constants.MessageConstant;
import com.ndh.ShopTechnology.dto.request.promotion.VolumePriceTierItemRequest;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.repository.ProductVolumePriceTierRepository;
import com.ndh.ShopTechnology.services.log.PriceEventHistoryService;
import com.ndh.ShopTechnology.services.promotion.ProductVolumePriceTierService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductVolumePriceTierServiceImpl implements ProductVolumePriceTierService {

    private final ProductVolumePriceTierRepository tierRepository;
    private final ProductVariantRepository variantRepository;
    private final PriceEventHistoryService priceEventHistoryService;

    public ProductVolumePriceTierServiceImpl(
            ProductVolumePriceTierRepository tierRepository,
            ProductVariantRepository variantRepository,
            PriceEventHistoryService priceEventHistoryService) {
        this.tierRepository = tierRepository;
        this.variantRepository = variantRepository;
        this.priceEventHistoryService = priceEventHistoryService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolumePriceTierResponse> listByVariant(Long productId, Long variantId) {
        ensureVariantBelongsToProduct(variantId, productId);
        return tierRepository.findByProductVariant_IdOrderByMinQuantityAsc(variantId).stream()
                .map(ProductVolumePriceTierServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolumePriceTierResponse> listByProduct(Long productId) {
        return tierRepository.findByProductIdForAdmin(productId).stream()
                .map(ProductVolumePriceTierServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolumePriceTierResponse> listAll() {
        return tierRepository.findAllForOverview().stream()
                .map(ProductVolumePriceTierServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * replaceTiers là thao tác bulk UPDATE – ghi log là UPDATE trên VOLUME_TIER,
     * idArgIndex=1 (variantId là đủ để identify).
     */
    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_VOLUME_TIER,
        action     = AdminActivityLogEntity.ACTION_UPDATE,
        idArgIndex = 1
    )
    public List<VolumePriceTierResponse> replaceTiers(
            Long productId, Long variantId, List<VolumePriceTierItemRequest> tiers) {
        ProductVariantEntity variant = ensureVariantBelongsToProduct(variantId, productId);
        if (tiers == null) tiers = List.of();

        Set<Integer> seen = new HashSet<>();
        for (VolumePriceTierItemRequest t : tiers) {
            if (!seen.add(t.getMinQuantity())) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Duplicate minQuantity in tier list: " + t.getMinQuantity());
            }
        }
        List<ProductVolumePriceTierEntity> existing =
                tierRepository.findByProductVariant_IdOrderByMinQuantityAsc(variantId);
        assertNoDropOfActiveVolumeTiers(existing, tiers);
        tierRepository.deleteAll(existing);

        List<ProductVolumePriceTierEntity> saved = new ArrayList<>();
        for (VolumePriceTierItemRequest t : tiers) {
            ProductVolumePriceTierEntity e = ProductVolumePriceTierEntity.builder()
                    .productVariant(variant)
                    .minQuantity(t.getMinQuantity())
                    .unitPrice(t.getUnitPrice())
                    .enabled(t.getEnabled() != null ? t.getEnabled() : true)
                    .build();
            saved.add(tierRepository.save(e));
        }
        List<VolumePriceTierResponse> result = saved.stream()
                .map(ProductVolumePriceTierServiceImpl::toResponse)
                .collect(Collectors.toList());
        priceEventHistoryService.logVolumeTierReplaced(variantId, productId);
        return result;
    }

    private static void assertNoDropOfActiveVolumeTiers(
            List<ProductVolumePriceTierEntity> existing,
            List<VolumePriceTierItemRequest> tiers) {
        Set<Integer> incomingMinQuantities = tiers.stream()
                .map(VolumePriceTierItemRequest::getMinQuantity)
                .collect(Collectors.toSet());
        boolean droppingActive = existing.stream()
                .anyMatch(e -> Boolean.TRUE.equals(e.getEnabled())
                        && !incomingMinQuantities.contains(e.getMinQuantity()));
        if (droppingActive) {
            throw new CustomApiException(HttpStatus.CONFLICT, MessageConstant.VOLUME_TIER_CANNOT_DROP_WHILE_ACTIVE);
        }
    }

    private ProductVariantEntity ensureVariantBelongsToProduct(Long variantId, Long productId) {
        return variantRepository.findById(variantId)
                .filter(v -> v.getProduct() != null && v.getProduct().getId().equals(productId))
                .orElseThrow(() -> new NotFoundEntityException(
                        "Variant " + variantId + " not found for product " + productId));
    }

    private static VolumePriceTierResponse toResponse(ProductVolumePriceTierEntity e) {
        ProductVariantEntity pv = e.getProductVariant();
        Long pid = pv != null && pv.getProduct() != null ? pv.getProduct().getId() : null;
        Long vid = pv != null ? pv.getId() : null;
        return VolumePriceTierResponse.builder()
                .id(e.getId())
                .productVariantId(vid)
                .productId(pid)
                .minQuantity(e.getMinQuantity())
                .unitPrice(e.getUnitPrice())
                .enabled(e.getEnabled())
                .build();
    }
}
