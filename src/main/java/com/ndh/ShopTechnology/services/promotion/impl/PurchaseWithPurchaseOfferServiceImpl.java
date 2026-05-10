package com.ndh.ShopTechnology.services.promotion.impl;

import com.ndh.ShopTechnology.dto.request.promotion.UpsertPurchaseWithPurchaseRequest;
import com.ndh.ShopTechnology.dto.response.promotion.PurchaseWithPurchaseOfferResponse;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.PurchaseWithPurchaseOfferRepository;
import com.ndh.ShopTechnology.services.promotion.PurchaseWithPurchaseOfferService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseWithPurchaseOfferServiceImpl implements PurchaseWithPurchaseOfferService {

    private final PurchaseWithPurchaseOfferRepository pwpRepository;
    private final ProductRepository productRepository;

    public PurchaseWithPurchaseOfferServiceImpl(
            PurchaseWithPurchaseOfferRepository pwpRepository,
            ProductRepository productRepository) {
        this.pwpRepository = pwpRepository;
        this.productRepository = productRepository;
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
    public PurchaseWithPurchaseOfferResponse create(UpsertPurchaseWithPurchaseRequest request) {
        validateIds(request.getAnchorProductId(), request.getCompanionProductId());
        if (pwpRepository.findByCompanionProduct_Id(request.getCompanionProductId()).isPresent()) {
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Companion product already has a purchase-with-purchase offer: " + request.getCompanionProductId());
        }
        ProductEntity anchor = loadProduct(request.getAnchorProductId());
        ProductEntity companion = loadProduct(request.getCompanionProductId());
        PurchaseWithPurchaseOfferEntity e = PurchaseWithPurchaseOfferEntity.builder()
                .anchorProduct(anchor)
                .companionProduct(companion)
                .promoUnitPrice(request.getPromoUnitPrice())
                .minAnchorQuantity(request.getMinAnchorQuantity() != null ? request.getMinAnchorQuantity() : 1)
                .companionPromoUnitsPerAnchor(
                        request.getCompanionPromoUnitsPerAnchor() != null ? request.getCompanionPromoUnitsPerAnchor() : 1)
                .maxCompanionPromoUnits(request.getMaxCompanionPromoUnits())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        e = pwpRepository.save(e);
        return toResponse(e);
    }

    @Override
    @Transactional
    public PurchaseWithPurchaseOfferResponse update(long id, UpsertPurchaseWithPurchaseRequest request) {
        PurchaseWithPurchaseOfferEntity e = pwpRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Purchase-with-purchase offer not found: " + id));
        validateIds(request.getAnchorProductId(), request.getCompanionProductId());
        pwpRepository.findByCompanionProduct_Id(request.getCompanionProductId()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new CustomApiException(HttpStatus.CONFLICT,
                        "Companion product already linked to another offer: " + request.getCompanionProductId());
            }
        });
        e.setAnchorProduct(loadProduct(request.getAnchorProductId()));
        e.setCompanionProduct(loadProduct(request.getCompanionProductId()));
        e.setPromoUnitPrice(request.getPromoUnitPrice());
        if (request.getMinAnchorQuantity() != null) {
            e.setMinAnchorQuantity(request.getMinAnchorQuantity());
        }
        if (request.getCompanionPromoUnitsPerAnchor() != null) {
            e.setCompanionPromoUnitsPerAnchor(request.getCompanionPromoUnitsPerAnchor());
        }
        e.setMaxCompanionPromoUnits(request.getMaxCompanionPromoUnits());
        if (request.getEnabled() != null) {
            e.setEnabled(request.getEnabled());
        }
        e = pwpRepository.save(e);
        return toResponse(e);
    }

    @Override
    @Transactional
    public void delete(long id) {
        if (!pwpRepository.existsById(id)) {
            throw new NotFoundEntityException("Purchase-with-purchase offer not found: " + id);
        }
        pwpRepository.deleteById(id);
    }

    private static void validateIds(Long anchorId, Long companionId) {
        if (anchorId.equals(companionId)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "anchorProductId and companionProductId must differ");
        }
    }

    private ProductEntity loadProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));
    }

    private static PurchaseWithPurchaseOfferResponse toResponse(PurchaseWithPurchaseOfferEntity e) {
        return PurchaseWithPurchaseOfferResponse.builder()
                .id(e.getId())
                .anchorProductId(e.getAnchorProduct().getId())
                .companionProductId(e.getCompanionProduct().getId())
                .promoUnitPrice(e.getPromoUnitPrice())
                .minAnchorQuantity(e.getMinAnchorQuantity())
                .companionPromoUnitsPerAnchor(e.getCompanionPromoUnitsPerAnchor())
                .maxCompanionPromoUnits(e.getMaxCompanionPromoUnits())
                .enabled(e.getEnabled())
                .build();
    }
}
