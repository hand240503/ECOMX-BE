package com.ndh.ShopTechnology.dto.response.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.log.PriceEventHistoryEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * DTO cho {@link PriceEventHistoryEntity} — lịch sử sự kiện chương trình giá.
 *
 * <p>Trả về từ {@code GET /admin/history/price-events}.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceEventHistoryResponse {

    private Long   id;
    private Date   createdAt;

    // ── Chương trình ──────────────────────────────────────────────────────

    /** PRICE_CHANGE | VOLUME_TIER | PWP_OFFER */
    private String programType;
    private String programTypeLabel;

    private Long   programId;

    // ── Sự kiện ───────────────────────────────────────────────────────────

    /** CREATED | UPDATED | DELETED | ENABLED | DISABLED | STARTED | ENDED | EXPIRED */
    private String eventType;
    private String eventTypeLabel;

    // ── Sản phẩm ─────────────────────────────────────────────────────────

    private Long   productId;
    private Long   productVariantId;

    // ── Giá trị trước / sau ───────────────────────────────────────────────

    private Double  oldBasePrice;
    private Double  newBasePrice;
    private Double  oldSalePrice;
    private Double  newSalePrice;

    private Integer oldQuantityLimit;
    private Integer newQuantityLimit;

    // ── Thời gian hiệu lực của chương trình (tại lúc ghi log) ────────────

    private Date   programStartAt;
    private Date   programEndAt;

    // ── Actor ─────────────────────────────────────────────────────────────

    private Long   actorUserId;
    private String actorUsername;
    private String actorFullName;

    // ── Ghi chú thay đổi ─────────────────────────────────────────────────

    private String note;

    // ── Factory ───────────────────────────────────────────────────────────

    public static PriceEventHistoryResponse fromEntity(PriceEventHistoryEntity e) {
        if (e == null) return null;

        String fullName = null;
        if (e.getActorUser() != null && e.getActorUser().getUserInfo() != null) {
            fullName = e.getActorUser().getUserInfo().getFullName();
        }

        return PriceEventHistoryResponse.builder()
                .id(e.getId())
                .createdAt(e.getCreatedAt())
                .programType(e.getProgramType())
                .programTypeLabel(programTypeLabel(e.getProgramType()))
                .programId(e.getProgramId())
                .eventType(e.getEventType())
                .eventTypeLabel(eventTypeLabel(e.getEventType()))
                .productId(e.getProductId())
                .productVariantId(e.getProductVariantId())
                .oldBasePrice(e.getOldBasePrice())
                .newBasePrice(e.getNewBasePrice())
                .oldSalePrice(e.getOldSalePrice())
                .newSalePrice(e.getNewSalePrice())
                .oldQuantityLimit(e.getOldQuantityLimit())
                .newQuantityLimit(e.getNewQuantityLimit())
                .programStartAt(e.getProgramStartAt())
                .programEndAt(e.getProgramEndAt())
                .actorUserId(e.getActorUser() != null ? e.getActorUser().getId() : null)
                .actorUsername(e.getActorUsername())
                .actorFullName(fullName)
                .note(e.getNote())
                .build();
    }

    // ── Label helpers ─────────────────────────────────────────────────────

    public static String programTypeLabel(String t) {
        if (t == null) return null;
        return switch (t) {
            case PriceEventHistoryEntity.PROGRAM_PRICE_CHANGE -> "Giá theo thời gian";
            case PriceEventHistoryEntity.PROGRAM_VOLUME_TIER  -> "Giá theo bậc SL";
            case PriceEventHistoryEntity.PROGRAM_PWP_OFFER    -> "Mua kèm (PwP)";
            default -> t;
        };
    }

    public static String eventTypeLabel(String e) {
        if (e == null) return null;
        return switch (e) {
            case PriceEventHistoryEntity.EVENT_CREATED  -> "Tạo mới";
            case PriceEventHistoryEntity.EVENT_UPDATED  -> "Cập nhật";
            case PriceEventHistoryEntity.EVENT_DELETED  -> "Xóa";
            case PriceEventHistoryEntity.EVENT_ENABLED  -> "Bật";
            case PriceEventHistoryEntity.EVENT_DISABLED -> "Tắt";
            case PriceEventHistoryEntity.EVENT_STARTED  -> "Bắt đầu (auto)";
            case PriceEventHistoryEntity.EVENT_ENDED    -> "Kết thúc (auto)";
            case PriceEventHistoryEntity.EVENT_EXPIRED  -> "Hết quota";
            default -> e;
        };
    }
}
