package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLogEntity, Long> {

    /** Lịch sử hoạt động của một user cụ thể */
    Page<AdminActivityLogEntity> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId, Pageable pageable);

    /** Lịch sử của một entity cụ thể (vd: product id = 5) */
    List<AdminActivityLogEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    /** Lọc theo action + entity_type */
    Page<AdminActivityLogEntity> findByActionAndEntityTypeOrderByCreatedAtDesc(
            String action, String entityType, Pageable pageable);

    /**
     * Tất cả log trong khoảng thời gian, có thể lọc thêm theo role code của actor.
     *
     * <p>Dùng {@code LEFT JOIN} (không FETCH) để tương thích với COUNT query sinh tự động.
     * Lazy-load userInfo sẽ được xử lý trong service khi chuyển sang DTO.
     *
     * @param actorRoleCode  Lọc theo role code của người thực hiện (vd: "EMPLOYEE"),
     *                       null = không lọc theo role.
     */
    @Query(value =
           "SELECT l FROM AdminActivityLogEntity l " +
           "LEFT JOIN FETCH l.actorUser u " +
           "LEFT JOIN u.role r " +
           "WHERE (:actorUserId  IS NULL OR u.id           = :actorUserId) " +
           "  AND (:entityType   IS NULL OR l.entityType   = :entityType) " +
           "  AND (:action       IS NULL OR l.action       = :action) " +
           "  AND (:from         IS NULL OR l.createdAt   >= :from) " +
           "  AND (:to           IS NULL OR l.createdAt   <= :to) " +
           "  AND (:actorRoleCode IS NULL OR r.code        = :actorRoleCode) " +
           "ORDER BY l.createdAt DESC",
           countQuery =
           "SELECT COUNT(l) FROM AdminActivityLogEntity l " +
           "LEFT JOIN l.actorUser u " +
           "LEFT JOIN u.role r " +
           "WHERE (:actorUserId  IS NULL OR u.id           = :actorUserId) " +
           "  AND (:entityType   IS NULL OR l.entityType   = :entityType) " +
           "  AND (:action       IS NULL OR l.action       = :action) " +
           "  AND (:from         IS NULL OR l.createdAt   >= :from) " +
           "  AND (:to           IS NULL OR l.createdAt   <= :to) " +
           "  AND (:actorRoleCode IS NULL OR r.code        = :actorRoleCode)")
    Page<AdminActivityLogEntity> search(
            @Param("actorUserId")   Long   actorUserId,
            @Param("entityType")    String entityType,
            @Param("action")        String action,
            @Param("from")          Date   from,
            @Param("to")            Date   to,
            @Param("actorRoleCode") String actorRoleCode,
            Pageable pageable);
}
