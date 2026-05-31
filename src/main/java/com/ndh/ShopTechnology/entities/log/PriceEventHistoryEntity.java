package com.ndh.ShopTechnology.entities.log;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * Ghi lại mọi sự kiện liên quan đến các chương trình giá:
 * flash-sale (PRICE_CHANGE), giá theo số lượng (VOLUME_TIER),
 * mua kèm khuyến mãi (PWP_OFFER).
 *
 * <p>Mỗi khi admin tạo / sửa / bật / tắt / xóa một chương trình giá,
 * hoặc khi job tự động kích hoạt / kết thúc chương trình, một record
 * sẽ được ghi vào bảng này.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "price_event_history", indexes = {
        @Index(name = "idx_peh_program",    columnList = "program_type, program_id"),
        @Index(name = "idx_peh_product",    columnList = "product_id"),
        @Index(name = "idx_peh_actor",      columnList = "actor_user_id"),
        @Index(name = "idx_peh_created_at", columnList = "created_at")
})
public class PriceEventHistoryEntity {

    // ── program_type constants ─────────────────────────────────────────────
    /** Đợt giá flash-sale / giá khuyến mãi theo thời gian */
    public static final String PROGRAM_PRICE_CHANGE = "PRICE_CHANGE";
    /** Giá theo bậc số lượng */
    public static final String PROGRAM_VOLUME_TIER  = "VOLUME_TIER";
    /** Chương trình mua kèm khuyến mãi */
    public static final String PROGRAM_PWP_OFFER    = "PWP_OFFER";

    // ── event_type constants ───────────────────────────────────────────────
    public static final String EVENT_CREATED  = "CREATED";
    public static final String EVENT_UPDATED  = "UPDATED";
    public static final String EVENT_DELETED  = "DELETED";
    public static final String EVENT_ENABLED  = "ENABLED";
    public static final String EVENT_DISABLED = "DISABLED";
    /** Chương trình tự động bắt đầu khi đến start_at (ghi bởi job) */
    public static final String EVENT_STARTED  = "STARTED";
    /** Chương trình tự động kết thúc khi đến end_at (ghi bởi job) */
    public static final String EVENT_ENDED    = "ENDED";
    /** Chương trình hết hạn do hết quota (ghi bởi hệ thống) */
    public static final String EVENT_EXPIRED  = "EXPIRED";

    // ── actor_username khi không có user thật ─────────────────────────────
    public static final String ACTOR_SYSTEM = "SYSTEM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // ── Loại & ID chương trình ────────────────────────────────────────────

    /** PRICE_CHANGE | VOLUME_TIER | PWP_OFFER */
    @Column(name = "program_type", nullable = false, length = 32)
    private String programType;

    /** FK đến bảng tương ứng (product_price_change.id / product_volume_price_tier.id / purchase_with_purchase_offer.id) */
    @Column(name = "program_id", nullable = false)
    private Long programId;

    // ── Sự kiện ───────────────────────────────────────────────────────────

    /** CREATED | UPDATED | DELETED | ENABLED | DISABLED | STARTED | ENDED | EXPIRED */
    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    // ── Sản phẩm liên quan ───────────────────────────────────────────────

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_variant_id")
    private Long productVariantId;

    // ── Giá trị trước / sau (cho PRICE_CHANGE) ───────────────────────────

    @Column(name = "old_base_price")
    private Double oldBasePrice;

    @Column(name = "new_base_price")
    private Double newBasePrice;

    @Column(name = "old_sale_price")
    private Double oldSalePrice;

    @Column(name = "new_sale_price")
    private Double newSalePrice;

    /** Giới hạn số lượng trước khi thay đổi */
    @Column(name = "old_quantity_limit")
    private Integer oldQuantityLimit;

    /** Giới hạn số lượng sau khi thay đổi */
    @Column(name = "new_quantity_limit")
    private Integer newQuantityLimit;

    /** Thời điểm start_at của chương trình tại lúc ghi log */
    @Column(name = "program_start_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date programStartAt;

    /** Thời điểm end_at của chương trình tại lúc ghi log */
    @Column(name = "program_end_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date programEndAt;

    // ── Actor ─────────────────────────────────────────────────────────────

    /** FK đến user thực hiện (null nếu job/system) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserEntity actorUser;

    /** Snapshot username tại thời điểm ghi log (tránh mất dữ liệu nếu user bị xóa) */
    @Column(name = "actor_username", length = 128)
    private String actorUsername;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdAt = new Date();
}
