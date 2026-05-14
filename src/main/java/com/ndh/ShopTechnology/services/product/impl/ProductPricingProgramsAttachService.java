package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceChangeResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductPurchaseWithPurchaseProgramResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductVariantResponse;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import com.ndh.ShopTechnology.repository.ProductVolumePriceTierRepository;
import com.ndh.ShopTechnology.repository.PurchaseWithPurchaseOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Gắn snapshot chương trình giá đang chạy (mix-and-match / volume tier, PWP, price change) vào DTO sản phẩm trả API.
 *
 * <p>Đơn giá hiển thị ({@link ProductVariantResponse#getEffectiveUnitPrice()}) đã resolve PC trước đó trong
 * {@link VariantDisplayPriceResolver}; trường {@link ProductVariantResponse#getActivePriceChange()} chỉ để FE hiển thị ngữ cảnh.
 */
@Component
@RequiredArgsConstructor
public class ProductPricingProgramsAttachService {

    private final ProductVolumePriceTierRepository volumeTierRepository;
    private final PurchaseWithPurchaseOfferRepository purchaseWithPurchaseOfferRepository;
    private final VariantDisplayPriceResolver variantDisplayPriceResolver;

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

        Date at = new Date();
        Map<Long, List<VolumePriceTierResponse>> tiersByProduct = loadVolumeTiers(productIds);
        Map<Long, List<ProductPurchaseWithPurchaseProgramResponse>> pwpByProduct = loadPwpPrograms(productIds);

        List<Long> variantIds = products.stream()
                .flatMap(p -> p.getVariants() == null ? Stream.of() : p.getVariants().stream())
                .filter(Objects::nonNull)
                .map(ProductVariantResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ProductPriceChangeEntity> pcByVariantId =
                variantDisplayPriceResolver.effectiveActivePriceChangesByVariantId(variantIds, at);

        for (ProductFullResponse p : products) {
            if (p.getId() == null) {
                continue;
            }
            p.setVolumePriceTiers(List.copyOf(tiersByProduct.getOrDefault(p.getId(), List.of())));
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
            }
        }
    }

    private Map<Long, List<VolumePriceTierResponse>> loadVolumeTiers(Collection<Long> productIds) {
        List<ProductVolumePriceTierEntity> rows =
                volumeTierRepository.findActiveFetchedByProductIdIn(productIds);
        Map<Long, List<VolumePriceTierResponse>> map = new LinkedHashMap<>();
        for (ProductVolumePriceTierEntity e : rows) {
            if (e.getProduct() == null || e.getProduct().getId() == null) {
                continue;
            }
            Long pid = e.getProduct().getId();
            map.computeIfAbsent(pid, k -> new ArrayList<>()).add(toVolumeTierResponse(e));
        }
        for (List<VolumePriceTierResponse> list : map.values()) {
            list.sort(Comparator.comparing(VolumePriceTierResponse::getMinQuantity,
                    Comparator.nullsLast(Integer::compareTo)));
        }
        return map;
    }

    private Map<Long, List<ProductPurchaseWithPurchaseProgramResponse>> loadPwpPrograms(Collection<Long> productIds) {
        Map<Long, List<ProductPurchaseWithPurchaseProgramResponse>> map = new LinkedHashMap<>();
        for (PurchaseWithPurchaseOfferEntity o :
                purchaseWithPurchaseOfferRepository.findActiveFetchedByCompanionProductIdIn(productIds)) {
            if (o.getCompanionProduct() == null || o.getCompanionProduct().getId() == null) {
                continue;
            }
            Long pid = o.getCompanionProduct().getId();
            map.computeIfAbsent(pid, k -> new ArrayList<>()).add(toPwpProgram(o, "companion"));
        }
        for (PurchaseWithPurchaseOfferEntity o :
                purchaseWithPurchaseOfferRepository.findActiveFetchedByAnchorProductIdIn(productIds)) {
            if (o.getAnchorProduct() == null || o.getAnchorProduct().getId() == null) {
                continue;
            }
            Long pid = o.getAnchorProduct().getId();
            map.computeIfAbsent(pid, k -> new ArrayList<>()).add(toPwpProgram(o, "anchor"));
        }
        return map;
    }

    private static VolumePriceTierResponse toVolumeTierResponse(ProductVolumePriceTierEntity e) {
        return VolumePriceTierResponse.builder()
                .id(e.getId())
                .productId(e.getProduct() != null ? e.getProduct().getId() : null)
                .minQuantity(e.getMinQuantity())
                .unitPrice(e.getUnitPrice())
                .enabled(e.getEnabled())
                .build();
    }

    private static ProductPurchaseWithPurchaseProgramResponse toPwpProgram(
            PurchaseWithPurchaseOfferEntity o, String role) {
        return ProductPurchaseWithPurchaseProgramResponse.builder()
                .role(role)
                .id(o.getId())
                .anchorProductId(o.getAnchorProduct() != null ? o.getAnchorProduct().getId() : null)
                .companionProductId(o.getCompanionProduct() != null ? o.getCompanionProduct().getId() : null)
                .promoUnitPrice(o.getPromoUnitPrice())
                .minAnchorQuantity(o.getMinAnchorQuantity())
                .companionPromoUnitsPerAnchor(o.getCompanionPromoUnitsPerAnchor())
                .maxCompanionPromoUnits(o.getMaxCompanionPromoUnits())
                .enabled(o.getEnabled())
                .build();
    }

    private static ProductPriceChangeResponse toPriceChangeResponse(ProductPriceChangeEntity e) {
        return ProductPriceChangeResponse.builder()
                .id(e.getId())
                .productVariantId(e.getProductVariant() != null ? e.getProductVariant().getId() : null)
                .basePrice(e.getBasePrice())
                .salePrice(e.getSalePrice())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .enabled(e.getEnabled())
                .build();
    }
}
