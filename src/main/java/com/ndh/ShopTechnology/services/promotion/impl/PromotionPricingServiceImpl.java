package com.ndh.ShopTechnology.services.promotion.impl;

import com.ndh.ShopTechnology.dto.request.order.CreateOrderDetailRequest;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import com.ndh.ShopTechnology.repository.ProductVolumePriceTierRepository;
import com.ndh.ShopTechnology.repository.PurchaseWithPurchaseOfferRepository;
import com.ndh.ShopTechnology.services.product.ProductEffectivePriceService;
import com.ndh.ShopTechnology.services.promotion.PromotionPricingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PromotionPricingServiceImpl implements PromotionPricingService {

    private final ProductVolumePriceTierRepository volumeTierRepository;
    private final PurchaseWithPurchaseOfferRepository pwpRepository;
    private final ProductEffectivePriceService effectivePriceService;

    public PromotionPricingServiceImpl(
            ProductVolumePriceTierRepository volumeTierRepository,
            PurchaseWithPurchaseOfferRepository pwpRepository,
            ProductEffectivePriceService effectivePriceService) {
        this.volumeTierRepository = volumeTierRepository;
        this.pwpRepository = pwpRepository;
        this.effectivePriceService = effectivePriceService;
    }

    @Override
    public List<PricedLine> priceLines(List<OrderVariantLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (OrderVariantLine ol : lines) {
            CreateOrderDetailRequest line = ol.line();
            ProductVariantEntity v = ol.variant();
            if (line == null || line.getQuantity() == null || v == null || v.getProduct() == null) {
                continue;
            }
            Long pid = v.getProduct().getId();
            qtyByProduct.merge(pid, line.getQuantity(), (a, b) -> {
                int x = a != null ? a : 0;
                int y = b != null ? b : 0;
                return x + y;
            });
        }

        List<Long> ids = new ArrayList<>(qtyByProduct.keySet());
        List<ProductVolumePriceTierEntity> allTiers = ids.isEmpty()
                ? List.of()
                : volumeTierRepository.findByProduct_IdInAndEnabledTrue(ids);
        Map<Long, List<ProductVolumePriceTierEntity>> tiersByProduct = allTiers.stream()
                .collect(Collectors.groupingBy(t -> t.getProduct().getId()));

        List<PurchaseWithPurchaseOfferEntity> pwpOffers = ids.isEmpty()
                ? List.of()
                : pwpRepository.findByCompanionProduct_IdInAndEnabledTrue(ids);
        Map<Long, PurchaseWithPurchaseOfferEntity> offerByCompanion = new HashMap<>();
        for (PurchaseWithPurchaseOfferEntity o : pwpOffers) {
            offerByCompanion.putIfAbsent(o.getCompanionProduct().getId(), o);
        }

        Map<Long, Integer> promoUnitsLeft = new HashMap<>();
        for (Map.Entry<Long, PurchaseWithPurchaseOfferEntity> e : offerByCompanion.entrySet()) {
            Long companionId = e.getKey();
            PurchaseWithPurchaseOfferEntity offer = e.getValue();
            long anchorId = offer.getAnchorProduct().getId();
            int anchorQ = qtyByProduct.getOrDefault(anchorId, 0);
            int companionQ = qtyByProduct.getOrDefault(companionId, 0);
            int promo = eligiblePromoUnits(offer, anchorQ, companionQ);
            if (promo > 0) {
                promoUnitsLeft.put(companionId, promo);
            }
        }

        Date now = new Date();
        List<PricedLine> out = new ArrayList<>();
        for (OrderVariantLine ol : lines) {
            CreateOrderDetailRequest line = ol.line();
            ProductVariantEntity variant = ol.variant();
            int q = line.getQuantity() != null ? line.getQuantity() : 0;
            Long productId = variant.getProduct().getId();
            int agg = qtyByProduct.getOrDefault(productId, q);
            double baseUnit = effectivePriceService.resolveEffectiveUnitPrice(variant, now);
            double volUnit = volumeTierUnitPrice(baseUnit, agg, tiersByProduct.get(productId));

            PurchaseWithPurchaseOfferEntity pwp = offerByCompanion.get(productId);
            if (pwp != null && promoUnitsLeft.containsKey(productId)) {
                int left = promoUnitsLeft.get(productId);
                int promoTake = Math.min(q, Math.max(0, left));
                promoUnitsLeft.put(productId, left - promoTake);
                int regularTake = q - promoTake;
                double lineTotal = promoTake * pwp.getPromoUnitPrice() + regularTake * volUnit;
                double unit = q > 0 ? lineTotal / q : 0.0;
                out.add(new PricedLine(line, unit, lineTotal));
            } else {
                double lineTotal = volUnit * q;
                out.add(new PricedLine(line, volUnit, lineTotal));
            }
        }
        return out;
    }

    private static int eligiblePromoUnits(PurchaseWithPurchaseOfferEntity offer, int anchorQty, int companionQty) {
        if (companionQty <= 0 || anchorQty <= 0) {
            return 0;
        }
        int minA = offer.getMinAnchorQuantity() != null ? offer.getMinAnchorQuantity() : 1;
        if (anchorQty < minA) {
            return 0;
        }
        int per = offer.getCompanionPromoUnitsPerAnchor() != null ? offer.getCompanionPromoUnitsPerAnchor() : 1;
        long cap = (long) anchorQty * per;
        if (offer.getMaxCompanionPromoUnits() != null) {
            cap = Math.min(cap, offer.getMaxCompanionPromoUnits());
        }
        return (int) Math.min(companionQty, cap);
    }

    private static double volumeTierUnitPrice(
            double fallbackUnitPrice,
            int aggregateQty,
            List<ProductVolumePriceTierEntity> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return fallbackUnitPrice;
        }
        return tiers.stream()
                .filter(t -> Boolean.TRUE.equals(t.getEnabled())
                        && t.getMinQuantity() != null
                        && aggregateQty >= t.getMinQuantity())
                .max(Comparator.comparingInt(ProductVolumePriceTierEntity::getMinQuantity))
                .map(ProductVolumePriceTierEntity::getUnitPrice)
                .filter(Objects::nonNull)
                .orElse(fallbackUnitPrice);
    }
}
