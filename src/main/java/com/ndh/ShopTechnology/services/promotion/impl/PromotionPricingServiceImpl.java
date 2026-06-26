package com.ndh.ShopTechnology.services.promotion.impl;

import com.ndh.ShopTechnology.dto.request.order.CreateOrderDetailRequest;
import com.ndh.ShopTechnology.dto.response.order.CheckoutPwpSuggestionDto;
import com.ndh.ShopTechnology.dto.response.order.OrderLinePricingProgramsDto;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import com.ndh.ShopTechnology.repository.ProductVolumePriceTierRepository;
import com.ndh.ShopTechnology.repository.PurchaseWithPurchaseOfferRepository;
import com.ndh.ShopTechnology.services.product.ProductEffectivePriceService;
import com.ndh.ShopTechnology.services.product.impl.ProductImageAttachService;
import com.ndh.ShopTechnology.services.product.impl.VariantDisplayPriceResolver;
import com.ndh.ShopTechnology.services.promotion.PromotionPricingService;
import com.ndh.ShopTechnology.utils.CatalogVariantUnitPrice;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PromotionPricingServiceImpl implements PromotionPricingService {

    private final ProductVolumePriceTierRepository volumeTierRepository;
    private final PurchaseWithPurchaseOfferRepository pwpRepository;
    private final ProductEffectivePriceService effectivePriceService;
    private final VariantDisplayPriceResolver variantDisplayPriceResolver;
    private final ProductImageAttachService productImageAttachService;

    public PromotionPricingServiceImpl(
            ProductVolumePriceTierRepository volumeTierRepository,
            PurchaseWithPurchaseOfferRepository pwpRepository,
            ProductEffectivePriceService effectivePriceService,
            VariantDisplayPriceResolver variantDisplayPriceResolver,
            ProductImageAttachService productImageAttachService) {
        this.volumeTierRepository = volumeTierRepository;
        this.pwpRepository = pwpRepository;
        this.effectivePriceService = effectivePriceService;
        this.variantDisplayPriceResolver = variantDisplayPriceResolver;
        this.productImageAttachService = productImageAttachService;
    }

    @Override
    public List<PricedLine> priceLines(List<OrderVariantLine> lines) {
        return priceLinesWithPrograms(lines, PricingContext.UNKNOWN).stream()
                .map(p -> new PricedLine(p.line(), p.finalUnitPrice(), p.lineTotal()))
                .toList();
    }

    @Override
    public List<PricedLineWithPrograms> priceLinesWithPrograms(List<OrderVariantLine> lines) {
        return priceLinesWithPrograms(lines, PricingContext.UNKNOWN);
    }

    @Override
    public List<PricedLineWithPrograms> priceLinesWithPrograms(
            List<OrderVariantLine> lines, PricingContext context) {
        return doPriceLinesWithPrograms(lines, context);
    }

    @Override
    public PricingWithSuggestionsResult priceLinesWithProgramsAndSuggestions(List<OrderVariantLine> lines) {
        return priceLinesWithProgramsAndSuggestions(lines, PricingContext.UNKNOWN);
    }

    @Override
    public PricingWithSuggestionsResult priceLinesWithProgramsAndSuggestions(
            List<OrderVariantLine> lines, PricingContext context) {
        List<PricedLineWithPrograms> pricedLines = doPriceLinesWithPrograms(lines, context);
        List<CheckoutPwpSuggestionDto> suggestions = buildPwpSuggestions(lines);
        return new PricingWithSuggestionsResult(pricedLines, suggestions);
    }

    private List<PricedLineWithPrograms> doPriceLinesWithPrograms(
            List<OrderVariantLine> lines, PricingContext context) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> qtyByVariant = new HashMap<>();
        for (OrderVariantLine ol : lines) {
            CreateOrderDetailRequest line = ol.line();
            ProductVariantEntity v = ol.variant();
            if (line == null || line.getQuantity() == null || v == null || v.getId() == null) continue;
            qtyByVariant.merge(v.getId(), line.getQuantity(), (a, b) -> {
                int x = a != null ? a : 0;
                int y = b != null ? b : 0;
                return x + y;
            });
        }

        List<Long> variantIds = new ArrayList<>(qtyByVariant.keySet());

        // Thời điểm định giá: lọc các chương trình theo khung thời gian áp dụng (start/end).
        Date now = new Date();

        List<ProductVolumePriceTierEntity> allTiers = variantIds.isEmpty()
                ? List.of()
                : volumeTierRepository.findByProductVariant_IdInAndEnabledTrue(variantIds);
        Map<Long, List<ProductVolumePriceTierEntity>> tiersByVariantId = allTiers.stream()
                .filter(t -> isWithinWindow(t.getStartAt(), t.getEndAt(), now))
                .collect(Collectors.groupingBy(t -> t.getProductVariant().getId()));

        List<PurchaseWithPurchaseOfferEntity> pwpOfferRows = new ArrayList<>();
        if (!variantIds.isEmpty()) {
            pwpOfferRows.addAll(pwpRepository.findActiveFetchedByCompanionVariantIdIn(variantIds));
            pwpOfferRows.addAll(pwpRepository.findActiveFetchedByAnchorVariantIdIn(variantIds));
        }
        Map<Long, PurchaseWithPurchaseOfferEntity> uniquePwpById = new LinkedHashMap<>();
        for (PurchaseWithPurchaseOfferEntity o : pwpOfferRows) {
            if (!isWithinWindow(o.getStartAt(), o.getEndAt(), now)) continue;
            uniquePwpById.putIfAbsent(o.getId(), o);
        }
        List<PurchaseWithPurchaseOfferEntity> pwpOffers = new ArrayList<>(uniquePwpById.values());

        Map<Long, Integer> promoPoolByOfferId = new HashMap<>();
        for (PurchaseWithPurchaseOfferEntity o : pwpOffers) {
            int anchorQ = anchorQtyForOffer(o, lines);
            int companionQ = companionQtyForOffer(o, lines);
            int promo = eligiblePromoUnits(o, anchorQ, companionQ);
            if (promo > 0) promoPoolByOfferId.put(o.getId(), promo);
        }

        long pricedAt = now.getTime();
        Map<Long, ProductPriceChangeEntity> pcByVariantId =
                variantDisplayPriceResolver.effectiveActivePriceChangesByVariantId(variantIds, now);

        String ctxCode = context.paymentMethodCode() != null
                ? context.paymentMethodCode().trim().toUpperCase()
                : null;

        List<PricedLineWithPrograms> out = new ArrayList<>();
        for (OrderVariantLine ol : lines) {
            CreateOrderDetailRequest line = ol.line();
            ProductVariantEntity variant = ol.variant();
            int q = line.getQuantity() != null ? line.getQuantity() : 0;
            int agg = qtyByVariant.getOrDefault(variant.getId(), q);

            double catalogUnit = CatalogVariantUnitPrice.resolve(variant);

            ProductPriceChangeEntity pc = pcByVariantId.get(variant.getId());
            if (pc != null && !isPcAllowedForPaymentMethod(pc, ctxCode)) {
                pc = null;
            }

            double effectiveBeforeTier = resolvePriceWithPc(variant, pc, now);

            VolumeTierPick volPick = pickVolumeTier(
                    effectiveBeforeTier, agg, tiersByVariantId.get(variant.getId()));
            double volUnit = volPick.unitPrice;

            OrderLinePricingProgramsDto.PriceChangeProgramDto priceChangeSnap = null;
            if (pc != null) {
                Double resolvedFromPc = pc.getSalePrice() != null ? pc.getSalePrice() : pc.getBasePrice();
                priceChangeSnap = OrderLinePricingProgramsDto.PriceChangeProgramDto.builder()
                        .id(pc.getId())
                        .productVariantId(variant.getId())
                        .basePrice(pc.getBasePrice())
                        .salePrice(pc.getSalePrice())
                        .resolvedUnitPrice(resolvedFromPc)
                        .startAtEpochMillis(epoch(pc.getStartAt()))
                        .endAtEpochMillis(epoch(pc.getEndAt()))
                        .quantityLimit(pc.getQuantityLimit())
                        .soldQuantity(pc.getSoldQuantity())
                        .requiredPaymentMethodCode(pc.getRequiredPaymentMethodCode())
                        .build();
            }

            OrderLinePricingProgramsDto.VolumeTierProgramDto volumeSnap = null;
            if (volPick.tier != null) {
                volumeSnap = OrderLinePricingProgramsDto.VolumeTierProgramDto.builder()
                        .id(volPick.tier.getId())
                        .minQuantity(volPick.tier.getMinQuantity())
                        .tierUnitPrice(volPick.tier.getUnitPrice())
                        .aggregateQuantityForVariantOnOrder(agg)
                        .build();
            }

            PurchaseWithPurchaseOfferEntity pwp = selectPwpForCompanionLine(ol, pwpOffers, promoPoolByOfferId);
            double finalUnit;
            double lineTotal;
            OrderLinePricingProgramsDto.PwpProgramDto pwpSnap = null;

            if (pwp != null) {
                int left = promoPoolByOfferId.getOrDefault(pwp.getId(), 0);
                int promoTake = Math.min(q, Math.max(0, left));
                promoPoolByOfferId.put(pwp.getId(), left - promoTake);
                int regularTake = q - promoTake;
                lineTotal = promoTake * pwp.getPromoUnitPrice() + regularTake * volUnit;
                finalUnit = q > 0 ? lineTotal / q : 0.0;
                pwpSnap = OrderLinePricingProgramsDto.PwpProgramDto.builder()
                        .offerId(pwp.getId())
                        .anchorProductId(pwp.getAnchorProduct().getId())
                        .companionProductId(pwp.getCompanionProduct().getId())
                        .anchorVariantId(pwp.getAnchorVariant() != null ? pwp.getAnchorVariant().getId() : null)
                        .companionVariantId(pwp.getCompanionVariant() != null ? pwp.getCompanionVariant().getId() : null)
                        .promoUnitPrice(pwp.getPromoUnitPrice())
                        .promoQuantity(promoTake)
                        .regularQuantity(regularTake)
                        .regularUnitPriceAfterPrograms(volUnit)
                        .build();
            } else {
                lineTotal = volUnit * q;
                finalUnit = volUnit;
            }

            OrderLinePricingProgramsDto programs = OrderLinePricingProgramsDto.builder()
                    .pricedAtEpochMillis(pricedAt)
                    .catalogUnitPrice(catalogUnit)
                    .effectiveUnitBeforeVolumeTier(effectiveBeforeTier)
                    .finalUnitPrice(finalUnit)
                    .lineTotal(lineTotal)
                    .priceChange(priceChangeSnap)
                    .volumeTier(volumeSnap)
                    .purchaseWithPurchase(pwpSnap)
                    .build();

            out.add(new PricedLineWithPrograms(line, finalUnit, lineTotal, programs));
        }
        return out;
    }

    private static boolean isPcAllowedForPaymentMethod(ProductPriceChangeEntity pc, String ctxCode) {
        String required = pc.getRequiredPaymentMethodCode();
        if (required == null || required.isBlank()) return true;
        if (ctxCode == null) return false;
        return required.equalsIgnoreCase(ctxCode);
    }

    private static double resolvePriceWithPc(ProductVariantEntity variant,
                                              ProductPriceChangeEntity pc, Date at) {
        if (pc != null) {
            if (pc.getSalePrice() != null) return pc.getSalePrice();
            if (pc.getBasePrice() != null) return pc.getBasePrice();
        }
        return CatalogVariantUnitPrice.resolve(variant);
    }

    private List<CheckoutPwpSuggestionDto> buildPwpSuggestions(List<OrderVariantLine> lines) {
        if (lines == null || lines.isEmpty()) return List.of();

        Set<Long> variantIdsInCart = lines.stream()
                .map(ol -> ol.variant() != null ? ol.variant().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<PurchaseWithPurchaseOfferEntity> anchorOffers =
                variantIdsInCart.isEmpty()
                        ? List.of()
                        : pwpRepository.findActiveFetchedByAnchorVariantIdIn(variantIdsInCart);

        if (anchorOffers.isEmpty()) return List.of();

        Date suggestNow = new Date();
        List<PurchaseWithPurchaseOfferEntity> eligibleOffers = new ArrayList<>();
        for (PurchaseWithPurchaseOfferEntity offer : anchorOffers) {
            if (!isWithinWindow(offer.getStartAt(), offer.getEndAt(), suggestNow)) continue;
            Long companionVarId = offer.getCompanionVariant() != null
                    ? offer.getCompanionVariant().getId() : null;
            if (companionVarId != null && variantIdsInCart.contains(companionVarId)) continue;
            int anchorQty = anchorQtyForOffer(offer, lines);
            int minA = offer.getMinAnchorQuantity() != null ? offer.getMinAnchorQuantity() : 1;
            if (anchorQty >= minA) eligibleOffers.add(offer);
        }

        if (eligibleOffers.isEmpty()) return List.of();

        List<Long> companionProductIds = eligibleOffers.stream()
                .map(o -> o.getCompanionProduct() != null ? o.getCompanionProduct().getId() : null)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> thumbnailByProductId =
                productImageAttachService.getPrimaryImageUrlsByProductIds(companionProductIds);

        List<CheckoutPwpSuggestionDto> result = new ArrayList<>();
        Date now = new Date();
        for (PurchaseWithPurchaseOfferEntity offer : eligibleOffers) {
            ProductVariantEntity companionVariant = offer.getCompanionVariant();
            if (companionVariant == null) continue;
            Long companionProductId = offer.getCompanionProduct() != null
                    ? offer.getCompanionProduct().getId() : null;
            String companionProductName = offer.getCompanionProduct() != null
                    ? offer.getCompanionProduct().getProductName() : null;
            String thumbnailUrl = companionProductId != null
                    ? thumbnailByProductId.get(companionProductId) : null;
            double companionRegularPrice = effectivePriceService
                    .resolveEffectiveUnitPrice(companionVariant, now);
            result.add(CheckoutPwpSuggestionDto.builder()
                    .offerId(offer.getId())
                    .anchorProductId(offer.getAnchorProduct() != null ? offer.getAnchorProduct().getId() : null)
                    .anchorVariantId(offer.getAnchorVariant() != null ? offer.getAnchorVariant().getId() : null)
                    .companionProductId(companionProductId)
                    .companionVariantId(companionVariant.getId())
                    .companionProductName(companionProductName)
                    .companionVariantSkuCode(companionVariant.getSkuCode())
                    .companionVariantOptions(companionVariant.getOptionValues())
                    .companionThumbnailUrl(thumbnailUrl)
                    .promoUnitPrice(offer.getPromoUnitPrice())
                    .companionRegularPrice(companionRegularPrice)
                    .minAnchorQuantity(offer.getMinAnchorQuantity())
                    .companionPromoUnitsPerAnchor(offer.getCompanionPromoUnitsPerAnchor())
                    .maxCompanionPromoUnits(offer.getMaxCompanionPromoUnits())
                    .build());
        }
        return result;
    }

    private record VolumeTierPick(double unitPrice, ProductVolumePriceTierEntity tier) {}

    private static VolumeTierPick pickVolumeTier(
            double fallbackUnitPrice, int aggregateQty,
            List<ProductVolumePriceTierEntity> tiers) {
        if (tiers == null || tiers.isEmpty()) return new VolumeTierPick(fallbackUnitPrice, null);
        return tiers.stream()
                .filter(t -> Boolean.TRUE.equals(t.getEnabled())
                        && t.getMinQuantity() != null
                        && aggregateQty >= t.getMinQuantity())
                .max(Comparator.comparingInt(ProductVolumePriceTierEntity::getMinQuantity))
                .map(t -> t.getUnitPrice() == null
                        ? new VolumeTierPick(fallbackUnitPrice, null)
                        : new VolumeTierPick(t.getUnitPrice(), t))
                .orElse(new VolumeTierPick(fallbackUnitPrice, null));
    }

    private static Long epoch(Date d) { return d != null ? d.getTime() : null; }

    /** Chương trình áp dụng tại thời điểm {@code now} nếu now ∈ [startAt, endAt]; null = không giới hạn đầu/cuối. */
    private static boolean isWithinWindow(Date startAt, Date endAt, Date now) {
        if (startAt != null && now.before(startAt)) return false;
        if (endAt != null && now.after(endAt)) return false;
        return true;
    }

    private static int eligiblePromoUnits(PurchaseWithPurchaseOfferEntity offer,
                                           int anchorQty, int companionQty) {
        if (companionQty <= 0 || anchorQty <= 0) return 0;
        int minA = offer.getMinAnchorQuantity() != null ? offer.getMinAnchorQuantity() : 1;
        if (anchorQty < minA) return 0;
        int per = offer.getCompanionPromoUnitsPerAnchor() != null
                ? offer.getCompanionPromoUnitsPerAnchor() : 1;
        long cap = (long) anchorQty * per;
        if (offer.getMaxCompanionPromoUnits() != null) cap = Math.min(cap, offer.getMaxCompanionPromoUnits());
        return (int) Math.min(companionQty, cap);
    }

    private static int lineQty(OrderVariantLine ol) {
        if (ol == null || ol.line() == null || ol.line().getQuantity() == null) return 0;
        return ol.line().getQuantity();
    }

    private static int anchorQtyForOffer(PurchaseWithPurchaseOfferEntity o, List<OrderVariantLine> lines) {
        Long avId = o.getAnchorVariant().getId();
        int sum = 0;
        for (OrderVariantLine ol : lines) {
            ProductVariantEntity v = ol.variant();
            if (v != null && v.getId().equals(avId)) sum += lineQty(ol);
        }
        return sum;
    }

    private static int companionQtyForOffer(PurchaseWithPurchaseOfferEntity o, List<OrderVariantLine> lines) {
        Long cvId = o.getCompanionVariant().getId();
        int sum = 0;
        for (OrderVariantLine ol : lines) {
            ProductVariantEntity v = ol.variant();
            if (v != null && v.getId().equals(cvId)) sum += lineQty(ol);
        }
        return sum;
    }

    private static PurchaseWithPurchaseOfferEntity selectPwpForCompanionLine(
            OrderVariantLine ol,
            List<PurchaseWithPurchaseOfferEntity> offers,
            Map<Long, Integer> promoPoolByOfferId) {
        ProductVariantEntity v = ol.variant();
        if (v == null) return null;
        return offers.stream()
                .filter(o -> o.getCompanionVariant().getId().equals(v.getId()))
                .filter(o -> promoPoolByOfferId.getOrDefault(o.getId(), 0) > 0)
                .findFirst().orElse(null);
    }
}
