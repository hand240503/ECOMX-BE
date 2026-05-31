package com.ndh.ShopTechnology.annotation;

import java.lang.annotation.*;

/**
 * Đánh dấu một service method cần được ghi vào admin_activity_log.
 *
 * <pre>
 * &#64;AdminAudit(entityType = AdminActivityLogEntity.ENTITY_BRAND, action = AdminActivityLogEntity.ACTION_CREATE)
 * public BrandResponse create(CreateBrandRequest request) { ... }
 *
 * &#64;AdminAudit(entityType = AdminActivityLogEntity.ENTITY_BRAND, action = AdminActivityLogEntity.ACTION_UPDATE,
 *             idArgIndex = 0, captureSnapshotBefore = true)
 * public BrandResponse update(long id, UpdateBrandRequest request) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminAudit {

    /** Loại entity: PRODUCT, BRAND, CATEGORY, PRICE_CHANGE, VOLUME_TIER, PWP_OFFER */
    String entityType();

    /** Hành động: CREATE, UPDATE, DELETE */
    String action();

    /**
     * Vị trí (index 0-based) của tham số chứa entity ID trong method signature.
     * Dùng để lấy snapshot trước khi UPDATE / DELETE.
     * Giá trị -1 = không có ID trong args (ví dụ CREATE).
     */
    int idArgIndex() default 0;

    /**
     * Có cần lấy snapshot trạng thái trước khi thực hiện thao tác không?
     * Thường bật với UPDATE và DELETE.
     */
    boolean captureSnapshotBefore() default false;
}
