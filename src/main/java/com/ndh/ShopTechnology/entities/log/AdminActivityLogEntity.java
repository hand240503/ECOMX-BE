package com.ndh.ShopTechnology.entities.log;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * Ghi lại mọi thao tác CREATE / UPDATE / DELETE của nhân viên (employee trở lên)
 * đối với các đối tượng quản lý: Product, Brand, Category, PriceChange, VolumeTier,
 * PurchaseWithPurchaseOffer.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "admin_activity_log", indexes = {
        @Index(name = "idx_aal_actor",       columnList = "actor_user_id"),
        @Index(name = "idx_aal_entity",      columnList = "entity_type, entity_id"),
        @Index(name = "idx_aal_created_at",  columnList = "created_at")
})
public class AdminActivityLogEntity {

    // ── action constants ────────────────────────────────────────────
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    // ── entity_type constants ───────────────────────────────────────
    public static final String ENTITY_PRODUCT       = "PRODUCT";
    public static final String ENTITY_BRAND         = "BRAND";
    public static final String ENTITY_CATEGORY      = "CATEGORY";
    public static final String ENTITY_PRICE_CHANGE  = "PRICE_CHANGE";
    public static final String ENTITY_VOLUME_TIER   = "VOLUME_TIER";
    public static final String ENTITY_PWP_OFFER     = "PWP_OFFER";
    public static final String ENTITY_ORDER         = "ORDER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Who ─────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserEntity actorUser;

    /** Snapshot username (giữ nguyên dù user đổi tên) */
    @Column(name = "actor_username", length = 128)
    private String actorUsername;

    // ── What ─────────────────────────────────────────────────────────

    /** CREATE | UPDATE | DELETE */
    @Column(name = "action", nullable = false, length = 16)
    private String action;

    /** PRODUCT | BRAND | CATEGORY | PRICE_CHANGE | VOLUME_TIER | PWP_OFFER | ORDER */
    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    /** ID của bản ghi bị tác động */
    @Column(name = "entity_id")
    private Long entityId;

    /** Tên / mô tả ngắn của đối tượng tại thời điểm log (vd: tên sản phẩm) */
    @Column(name = "entity_label", length = 512)
    private String entityLabel;

    /** Snapshot JSON trạng thái trước khi thay đổi (null với CREATE) */
    @Column(name = "snapshot_before", columnDefinition = "TEXT")
    private String snapshotBefore;

    /** Snapshot JSON trạng thái sau khi thay đổi (null với DELETE) */
    @Column(name = "snapshot_after", columnDefinition = "TEXT")
    private String snapshotAfter;

    // ── Context ──────────────────────────────────────────────────────

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdAt = new Date();
}
