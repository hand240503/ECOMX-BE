package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceChangeResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductPurchaseWithPurchaseProgramResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductVariantResponse;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import com.ndh.ShopTechnology.repository.ProductVolumePriceTierRepository;
import com.ndh.ShopTechnology.repository.PurchaseWithPurchaseOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ProductPricingProgramsAttachService {

    private final ProductVolumePriceTierRepository volumeTierRepository;
    private final PurchaseWithPurchaseOfferRepository purchaseWithPurchaseOfferRepository;
    private final VariantDisplayPriceResolver variantDisplayPriceResolver;
    private final ProductImageAttachService productImageAttachService;

    public void attach(List<ProductFullResponse> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> productIds = products.stream()
                .map(ProductFullResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return;
        }

        List<Long> variantIds = products.stream()
                .flatMap(p -> p.getVariants() == null ? Stream.of() : p.getVariants().stream())
                .filter(Objects::nonNull)
                .map(ProductVariantResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Date at = new Date();
        Map<Long, List<VolumePriceTierResponse>> tiersByVariant = loadVolumeTiersByVariantId(variantIds);
        Map<Long, List<ProductPurchaseWithPurchaseProgramResponse>> pwpByProduct =
                loadPwpPrograms(products, variantIds);

        Map<Long, ProductPriceChangeEntity> pcByVariantId =
                variantDisplayPriceResolver.effectiveActivePriceChangesByVariantId(variantIds, at);

        for (ProductFullResponse p : products) {
            if (p.getId() == null) {
                continue;
            }
            p.setVolumePriceTiers(null);
            p.setPurchaseWithPurchasePrograms(List.copyOf(pwpByProduct.getOrDefault(p.getId(), List.of())));
            if (p.getVariants() == null) {
                continue;
            }
            for (ProductVariantResponse v : p.getVariants()) {
                if (v == null || v.getId() == null) {
                    continue;
                }
                ProductPriceChangeEntity pc = pcByVariantId.get(v.getId());
                v.setActivePriceChange(pc != null ? toPriceChangeResponse(pc) : null);
                v.setVolumePriceTiers(List.copyOf(tiersByVariant.getOrDefault(v.getId(), List.of())));
            }
        }
    }

    private Map<Long, List<VolumePriceTierResponse>> loadVolumeTiersByVariantId(List<Long> variantIds) {
        Map<Long, List<VolumePriceTierResponse>> map = new LinkedHashMap<>();
        if (variantIds == null || variantIds.isEmpty()) {
            return map;
        }
        List<ProductVolumePriceTierEntity> rows = volumeTierRepository.findByProductVariant_IdIn(variantIds);
        for (ProductVolumePriceTierEntity e : rows) {
            if (e.getProductVariant() == null || e.getProductVariant().getId() == null) {
                continue;
            }
            Long vid = e.getProductVariant().getId();
            map.computeIfAbsent(vid, k -> new ArrayList<>()).add(toVolumeTierResponse(e));
        }
        for (List<VolumePriceTierResponse> list : map.values()) {
            list.sort(Comparator.comparing(VolumePriceTierResponse::getMinQuantity,
                    Comparator.nullsLast(Integer::compareTo)));
        }
        return map;
    }

    private Map<Long, List<ProductPurchaseWithPurchaseProgramResponse>> loadPwpPrograms(
            List<ProductFullResponse> products, List<Long> variantIds) {
        Map<Long, List<ProductPurchaseWithPurchaseProgramResponse>> map = new LinkedHashMap<>();
        Set<Long> productIdSet =
                products.stream().map(ProductFullResponse::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (variantIds == null || variantIds.isEmpty()) {
            return map;
        }
        Set<PurchaseWithPurchaseOfferEntity> offers = new LinkedHashSet<>();
        offers.addAll(purchaseWithPurchaseOfferRepository.findActiveFetchedByCompanionVariantIdIn(variantIds));
        offers.addAll(purchaseWithPurchaseOfferRepository.findActiveFetchedByAnchorVariantIdIn(variantIds));

        Set<Long> relatedProductIds = new LinkedHashSet<>();
        for (PurchaseWithPurchaseOfferEntity o : offers) {
            if (o.getCompanionProduct() != null && o.getCompanionProduct().getId() != null) {
                relatedProductIds.add(o.getCompanionProduct().getId());
            }
            if (o.getAnchorProduct() != null && o.getAnchorProduct().getId() != null) {
                relatedProductIds.add(o.getAnchorProduct().getId());
            }
        }
        Map<Long, String> imageUrls = productImageAttachService.getPrimaryImageUrlsByProductIds(relatedProductIds);

        for (PurchaseWithPurchaseOfferEntity o : offers) {
            String anchorImage = o.getAnchorProduct() != null ? imageUrls.get(o.getAnchorProduct().getId()) : null;
            String companionImage = o.getCompanionProduct() != null ? imageUrls.get(o.getCompanionProduct().getId()) : null;

            if (o.getCompanionProduct() != null && o.getCompanionProduct().getId() != null
                    && productIdSet.contains(o.getCompanionProduct().getId())) {
                map.computeIfAbsent(o.getCompanionProduct().getId(), k -> new ArrayList<>())
                        .add(toPwpProgram(o, "companion", anchorImage, companionImage));
            }
            if (o.getAnchorProduct() != null && o.getAnchorProduct().getId() != null
                    && productIdSet.contains(o.getAnchorProduct().getId())) {
                map.computeIfAbsent(o.getAnchorProduct().getId(), k -> new ArrayList<>())
                        .add(toPwpProgram(o, "anchor", anchorImage, companionImage));
            }
        }
        return map;
    }

    private static VolumePriceTierResponse toVolumeTierResponse(ProductVolumePriceTierEntity e) {
        ProductVariantEntity pv = e.getProductVariant();
        Long vid = pv != null ? pv.getId() : null;
        Long pid = pv != null && pv.getProduct() != null ? pv.getProduct().getId() : null;
        return VolumePriceTierResponse.builder()
                .id(e.getId())
                .productVariantId(vid)
                .productId(pid)
                .minQuantity(e.getMinQuantity())
                .unitPrice(e.getUnitPrice())
                .enabled(e.getEnabled())
                .build();
    }

    private static ProductPurchaseWithPurchaseProgramResponse toPwpProgram(
            PurchaseWithPurchaseOfferEntity o, String role, String anchorImage, String companionImage) {
        return ProductPurchaseWithPurchaseProgramResponse.builder()
                .role(role)
                .id(o.getId())
                .anchorProductId(o.getAnchorProduct() != null ? o.getAnchorProduct().getId() : null)
                .anchorProductName(o.getAnchorProduct() != null ? o.getAnchorProduct().getProductName() : null)
                .companionProductId(o.getCompanionProduct() != null ? o.getCompanionProduct().getId() : null)
                .companionProductName(o.getCompanionProduct() != null ? o.getCompanionProduct().getProductName() : null)
                .anchorVariantId(o.getAnchorVariant() != null ? o.getAnchorVariant().getId() : null)
                .companionVariantId(o.getCompanionVariant() != null ? o.getCompanionVariant().getId() : null)
                .promoUnitPrice(o.getPromoUnitPrice())
                .minAnchorQuantity(o.getMinAnchorQuantity())
                .companionPromoUnitsPerAnchor(o.getCompanionPromoUnitsPerAnchor())
                .maxCompanionPromoUnits(o.getMaxCompanionPromoUnits())
                .anchorProductMainImageUrl(anchorImage)
                .companionProductMainImageUrl(companionImage)
                .enabled(o.getEnabled())
                .build();
    }

    private static ProductPriceChangeResponse toPriceChangeResponse(ProductPriceChangeEntity e) {
        return com.ndh.ShopTechnology.services.product.impl.ProductPriceChangeServiceImpl.toResponse(e);
    }
}
