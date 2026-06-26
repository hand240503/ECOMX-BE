package com.ndh.ShopTechnology.services.promotion.impl;

import com.ndh.ShopTechnology.annotation.AdminAudit;
import com.ndh.ShopTechnology.constants.MessageConstant;
import com.ndh.ShopTechnology.dto.request.promotion.UpsertPurchaseWithPurchaseRequest;
import com.ndh.ShopTechnology.dto.response.promotion.PurchaseWithPurchaseOfferResponse;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.repository.PurchaseWithPurchaseOfferRepository;
import com.ndh.ShopTechnology.services.log.PriceEventHistoryService;
import com.ndh.ShopTechnology.services.promotion.PurchaseWithPurchaseOfferService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseWithPurchaseOfferServiceImpl implements PurchaseWithPurchaseOfferService {

    private final PurchaseWithPurchaseOfferRepository pwpRepository;
    private final ProductVariantRepository variantRepository;
    private final PriceEventHistoryService priceEventHistoryService;

    public PurchaseWithPurchaseOfferServiceImpl(
            PurchaseWithPurchaseOfferRepository pwpRepository,
            ProductVariantRepository variantRepository,
            PriceEventHistoryService priceEventHistoryService) {
        this.pwpRepository = pwpRepository;
        this.variantRepository = variantRepository;
        this.priceEventHistoryService = priceEventHistoryService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseWithPurchaseOfferResponse> listAll() {
        return pwpRepository.findAllByOrderByIdAsc().stream()
                .map(PurchaseWithPurchaseOfferServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_PWP_OFFER,
        action     = AdminActivityLogEntity.ACTION_CREATE,
        idArgIndex = -1
    )
    public PurchaseWithPurchaseOfferResponse create(UpsertPurchaseWithPurchaseRequest request) {
        validateIds(request.getAnchorProductId(), request.getCompanionProductId());
        ProductVariantEntity anchorVar = loadVariantStrict(
                request.getAnchorVariantId(), request.getAnchorProductId(), "anchor");
        ProductVariantEntity companionVar = loadVariantStrict(
                request.getCompanionVariantId(), request.getCompanionProductId(), "companion");
        assertCompanionVariantFree(request.getCompanionVariantId(), null);
        PurchaseWithPurchaseOfferEntity e = PurchaseWithPurchaseOfferEntity.builder()
                .anchorProduct(anchorVar.getProduct())
                .anchorVariant(anchorVar)
                .companionProduct(companionVar.getProduct())
                .companionVariant(companionVar)
                .promoUnitPrice(request.getPromoUnitPrice())
                .minAnchorQuantity(request.getMinAnchorQuantity() != null ? request.getMinAnchorQuantity() : 1)
                .companionPromoUnitsPerAnchor(
                        request.getCompanionPromoUnitsPerAnchor() != null ? request.getCompanionPromoUnitsPerAnchor() : 1)
                .maxCompanionPromoUnits(request.getMaxCompanionPromoUnits())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();
        e = pwpRepository.save(e);
        priceEventHistoryService.logPwpCreated(e);
        return toResponse(e);
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_PWP_OFFER,
        action     = AdminActivityLogEntity.ACTION_UPDATE,
        idArgIndex = 0
    )
    public PurchaseWithPurchaseOfferResponse update(long id, UpsertPurchaseWithPurchaseRequest request) {
        PurchaseWithPurchaseOfferEntity e = pwpRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Purchase-with-purchase offer not found: " + id));
        validateIds(request.getAnchorProductId(), request.getCompanionProductId());
        ProductVariantEntity anchorVar = loadVariantStrict(
                request.getAnchorVariantId(), request.getAnchorProductId(), "anchor");
        ProductVariantEntity companionVar = loadVariantStrict(
                request.getCompanionVariantId(), request.getCompanionProductId(), "companion");
        assertCompanionVariantFree(request.getCompanionVariantId(), id);
        e.setAnchorProduct(anchorVar.getProduct());
        e.setAnchorVariant(anchorVar);
        e.setCompanionProduct(companionVar.getProduct());
        e.setCompanionVariant(companionVar);
        e.setPromoUnitPrice(request.getPromoUnitPrice());
        if (request.getMinAnchorQuantity() != null)          e.setMinAnchorQuantity(request.getMinAnchorQuantity());
        if (request.getCompanionPromoUnitsPerAnchor() != null) e.setCompanionPromoUnitsPerAnchor(request.getCompanionPromoUnitsPerAnchor());
        e.setMaxCompanionPromoUnits(request.getMaxCompanionPromoUnits());
        if (request.getEnabled() != null) e.setEnabled(request.getEnabled());
        e.setStartAt(request.getStartAt());
        e.setEndAt(request.getEndAt());
        e = pwpRepository.save(e);
        priceEventHistoryService.logPwpUpdated(e);
        return toResponse(e);
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_PWP_OFFER,
        action     = AdminActivityLogEntity.ACTION_DELETE,
        idArgIndex = 0
    )
    public void delete(long id) {
        PurchaseWithPurchaseOfferEntity e = pwpRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Purchase-with-purchase offer not found: " + id));
        if (Boolean.TRUE.equals(e.getEnabled())) {
            throw new CustomApiException(HttpStatus.CONFLICT, MessageConstant.PWP_CANNOT_DELETE_WHILE_ACTIVE);
        }
        pwpRepository.delete(e);
        priceEventHistoryService.logPwpDeleted(e);
    }

    private void assertCompanionVariantFree(Long companionVariantId, Long excludeOfferId) {
        pwpRepository.findByCompanionVariant_Id(companionVariantId).ifPresent(o -> {
            if (excludeOfferId == null || !o.getId().equals(excludeOfferId)) {
                throw new CustomApiException(HttpStatus.CONFLICT,
                        "Companion variant already has a PwP: " + companionVariantId);
            }
        });
    }

    private static void validateIds(Long anchorId, Long companionId) {
        if (anchorId.equals(companionId)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "anchorProductId and companionProductId must differ");
        }
    }

    private ProductVariantEntity loadVariantStrict(Long variantId, Long productId, String role) {
        ProductVariantEntity v = variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundEntityException(role + " variant not found: " + variantId));
        if (v.getProduct() == null || !v.getProduct().getId().equals(productId)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    role + " variant " + variantId + " does not belong to product " + productId);
        }
        return v;
    }

    private static PurchaseWithPurchaseOfferResponse toResponse(PurchaseWithPurchaseOfferEntity e) {
        return PurchaseWithPurchaseOfferResponse.builder()
                .id(e.getId())
                .anchorProductId(e.getAnchorProduct().getId())
                .companionProductId(e.getCompanionProduct().getId())
                .anchorVariantId(e.getAnchorVariant().getId())
                .companionVariantId(e.getCompanionVariant().getId())
                .promoUnitPrice(e.getPromoUnitPrice())
                .minAnchorQuantity(e.getMinAnchorQuantity())
                .companionPromoUnitsPerAnchor(e.getCompanionPromoUnitsPerAnchor())
                .maxCompanionPromoUnits(e.getMaxCompanionPromoUnits())
                .enabled(e.getEnabled())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .build();
    }
}
