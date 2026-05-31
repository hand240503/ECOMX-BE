package com.ndh.ShopTechnology.services.log.impl;

import com.ndh.ShopTechnology.entities.log.PriceEventHistoryEntity;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.PriceEventHistoryRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.log.PriceEventHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceEventHistoryServiceImpl implements PriceEventHistoryService {

    private final PriceEventHistoryRepository repository;
    private final UserRepository userRepository;

    // ── PRICE_CHANGE ──────────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPriceChangeCreated(ProductPriceChangeEntity saved) {
        save(PriceEventHistoryEntity.builder()
                .programType(PriceEventHistoryEntity.PROGRAM_PRICE_CHANGE)
                .programId(saved.getId())
                .eventType(PriceEventHistoryEntity.EVENT_CREATED)
                .productId(saved.getProductId())
                .productVariantId(variantId(saved))
                .newBasePrice(saved.getBasePrice())
                .newSalePrice(saved.getSalePrice())
                .newQuantityLimit(saved.getQuantityLimit())
                .programStartAt(saved.getStartAt())
                .programEndAt(saved.getEndAt())
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPriceChangeUpdated(ProductPriceChangeEntity before, ProductPriceChangeEntity after) {
        // Nếu chỉ thay đổi enabled → dùng event ENABLED / DISABLED thay vì UPDATED
        boolean enabledChanged = before.getEnabled() != null
                && !before.getEnabled().equals(after.getEnabled());
        String eventType = enabledChanged
                ? (Boolean.TRUE.equals(after.getEnabled())
                        ? PriceEventHistoryEntity.EVENT_ENABLED
                        : PriceEventHistoryEntity.EVENT_DISABLED)
                : PriceEventHistoryEntity.EVENT_UPDATED;

        save(PriceEventHistoryEntity.builder()
                .programType(PriceEventHistoryEntity.PROGRAM_PRICE_CHANGE)
                .programId(after.getId())
                .eventType(eventType)
                .productId(after.getProductId())
                .productVariantId(variantId(after))
                .oldBasePrice(before.getBasePrice())
                .newBasePrice(after.getBasePrice())
                .oldSalePrice(before.getSalePrice())
                .newSalePrice(after.getSalePrice())
                .oldQuantityLimit(before.getQuantityLimit())
                .newQuantityLimit(after.getQuantityLimit())
                .programStartAt(after.getStartAt())
                .programEndAt(after.getEndAt())
                .note(buildUpdateNote(before, after))
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPriceChangeDeleted(ProductPriceChangeEntity deleted) {
        save(PriceEventHistoryEntity.builder()
                .programType(PriceEventHistoryEntity.PROGRAM_PRICE_CHANGE)
                .programId(deleted.getId())
                .eventType(PriceEventHistoryEntity.EVENT_DELETED)
                .productId(deleted.getProductId())
                .productVariantId(variantId(deleted))
                .oldBasePrice(deleted.getBasePrice())
                .oldSalePrice(deleted.getSalePrice())
                .oldQuantityLimit(deleted.getQuantityLimit())
                .programStartAt(deleted.getStartAt())
                .programEndAt(deleted.getEndAt())
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPriceChangeSystemEvent(ProductPriceChangeEntity entity, String eventType) {
        PriceEventHistoryEntity entry = PriceEventHistoryEntity.builder()
                .programType(PriceEventHistoryEntity.PROGRAM_PRICE_CHANGE)
                .programId(entity.getId())
                .eventType(eventType)
                .productId(entity.getProductId())
                .productVariantId(variantId(entity))
                .programStartAt(entity.getStartAt())
                .programEndAt(entity.getEndAt())
                .actorUsername(PriceEventHistoryEntity.ACTOR_SYSTEM)
                .build();
        // System event — không gán actorUser
        saveRaw(entry);
    }

    // ── VOLUME_TIER ───────────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logVolumeTierReplaced(Long variantId, Long productId) {
        save(PriceEventHistoryEntity.builder()
                .programType(PriceEventHistoryEntity.PROGRAM_VOLUME_TIER)
                .programId(variantId)          // dùng variantId làm program_id (không có id tier riêng cho bulk)
                .eventType(PriceEventHistoryEntity.EVENT_UPDATED)
                .productVariantId(variantId)
                .productId(productId)
                .build());
    }

    // ── PWP_OFFER ─────────────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPwpCreated(PurchaseWithPurchaseOfferEntity saved) {
        save(PriceEventHistoryEntity.builder()
                .programType(PriceEventHistoryEntity.PROGRAM_PWP_OFFER)
                .programId(saved.getId())
                .eventType(PriceEventHistoryEntity.EVENT_CREATED)
                .productId(saved.getAnchorProduct() != null ? saved.getAnchorProduct().getId() : null)
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPwpUpdated(PurchaseWithPurchaseOfferEntity after) {
        save(PriceEventHistoryEntity.builder()
                .programType(PriceEventHistoryEntity.PROGRAM_PWP_OFFER)
                .programId(after.getId())
                .eventType(PriceEventHistoryEntity.EVENT_UPDATED)
                .productId(after.getAnchorProduct() != null ? after.getAnchorProduct().getId() : null)
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPwpDeleted(PurchaseWithPurchaseOfferEntity deleted) {
        save(PriceEventHistoryEntity.builder()
                .programType(PriceEventHistoryEntity.PROGRAM_PWP_OFFER)
                .programId(deleted.getId())
                .eventType(PriceEventHistoryEntity.EVENT_DELETED)
                .productId(deleted.getAnchorProduct() != null ? deleted.getAnchorProduct().getId() : null)
                .build());
    }

    // ── Query ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PriceEventHistoryEntity> search(String programType, Long programId,
                                                 String eventType, Long productId,
                                                 Date from, Date to, Pageable pageable) {
        return repository.search(programType, programId, eventType, productId, from, to, pageable);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /**
     * Ghi log với actor lấy từ SecurityContext (admin đang thực hiện thao tác).
     * Dùng Propagation.REQUIRES_NEW để lỗi audit không rollback transaction chính.
     */
    private void save(PriceEventHistoryEntity entry) {
        try {
            String username = resolveCurrentUsername();
            UserEntity actor = resolveCurrentUser(username);
            entry.setActorUser(actor);
            entry.setActorUsername(username);
            saveRaw(entry);
        } catch (Exception e) {
            log.error("[PriceEventHistory] Failed to save log entry", e);
        }
    }

    private void saveRaw(PriceEventHistoryEntity entry) {
        try {
            repository.save(entry);
        } catch (Exception e) {
            log.error("[PriceEventHistory] Failed to persist entry: programType={} programId={} eventType={}",
                    entry.getProgramType(), entry.getProgramId(), entry.getEventType(), e);
        }
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return PriceEventHistoryEntity.ACTOR_SYSTEM;
        String name = auth.getName();
        return (name == null || "anonymousUser".equals(name))
                ? PriceEventHistoryEntity.ACTOR_SYSTEM : name;
    }

    private UserEntity resolveCurrentUser(String username) {
        if (username == null || PriceEventHistoryEntity.ACTOR_SYSTEM.equals(username)) return null;
        return userRepository.findOneByUsername(username).orElse(null);
    }

    private static Long variantId(ProductPriceChangeEntity e) {
        ProductVariantEntity v = e.getProductVariant();
        return v != null ? v.getId() : null;
    }

    /**
     * Tạo chuỗi mô tả chi tiết các trường thay đổi.
     * Ví dụ: "Giá ưu đãi: 31,900,000 → 32,000,000 | Thời gian kết thúc: 28/05/2026 16:30 → 29/05/2026 16:30"
     */
    private static String buildUpdateNote(ProductPriceChangeEntity before, ProductPriceChangeEntity after) {
        List<String> parts = new ArrayList<>();
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // Giá ưu đãi
        if (!Objects.equals(before.getSalePrice(), after.getSalePrice())) {
            parts.add("Giá ưu đãi: " + fmtPrice(before.getSalePrice(), nf)
                    + " → " + fmtPrice(after.getSalePrice(), nf));
        }
        // Thời gian bắt đầu
        if (!Objects.equals(before.getStartAt(), after.getStartAt())) {
            parts.add("Bắt đầu: " + fmtDate(before.getStartAt(), sdf)
                    + " → " + fmtDate(after.getStartAt(), sdf));
        }
        // Thời gian kết thúc
        if (!Objects.equals(before.getEndAt(), after.getEndAt())) {
            parts.add("Kết thúc: " + fmtDate(before.getEndAt(), sdf)
                    + " → " + fmtDate(after.getEndAt(), sdf));
        }
        // Tổng số lượng (quota)
        if (!Objects.equals(before.getQuantityLimit(), after.getQuantityLimit())) {
            parts.add("Số lượng: " + fmtInt(before.getQuantityLimit())
                    + " → " + fmtInt(after.getQuantityLimit()));
        }
        // Tối đa mỗi khách
        if (!Objects.equals(before.getMaxPerCustomer(), after.getMaxPerCustomer())) {
            parts.add("Tối đa/khách: " + fmtInt(before.getMaxPerCustomer())
                    + " → " + fmtInt(after.getMaxPerCustomer()));
        }
        // Phương thức thanh toán
        if (!Objects.equals(before.getRequiredPaymentMethodCode(), after.getRequiredPaymentMethodCode())) {
            parts.add("Phương thức TT: " + fmtStr(before.getRequiredPaymentMethodCode())
                    + " → " + fmtStr(after.getRequiredPaymentMethodCode()));
        }
        // Kích hoạt / tạm dừng
        if (!Objects.equals(before.getEnabled(), after.getEnabled())) {
            parts.add("Trạng thái: " + fmtEnabled(before.getEnabled())
                    + " → " + fmtEnabled(after.getEnabled()));
        }

        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private static String fmtPrice(Double v, NumberFormat nf) {
        return v == null ? "—" : nf.format(v.longValue());
    }

    private static String fmtDate(java.util.Date d, SimpleDateFormat sdf) {
        return d == null ? "—" : sdf.format(d);
    }

    private static String fmtInt(Integer v) {
        return v == null ? "không giới hạn" : String.valueOf(v);
    }

    private static String fmtStr(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private static String fmtEnabled(Boolean v) {
        if (v == null) return "—";
        return Boolean.TRUE.equals(v) ? "Kích hoạt" : "Tạm dừng";
    }
}
