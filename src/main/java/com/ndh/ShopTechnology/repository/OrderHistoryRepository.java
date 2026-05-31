package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.log.OrderHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistoryEntity, Long> {

    /** Lấy toàn bộ lịch sử của một đơn hàng, mới nhất trước */
    List<OrderHistoryEntity> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /** Lấy lịch sử theo loại thay đổi của một đơn hàng */
    List<OrderHistoryEntity> findByOrderIdAndChangeTypeOrderByCreatedAtDesc(Long orderId, String changeType);

    /**
     * Lấy lịch sử ORDER_STATUS của một đơn, kèm changedByUser và role để tránh N+1.
     * Dùng để xác định ai đã hủy đơn (fallback khi cancelledBy null).
     */
    @Query("SELECT h FROM OrderHistoryEntity h " +
           "LEFT JOIN FETCH h.changedByUser u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE h.order.id = :orderId " +
           "  AND h.changeType = :changeType " +
           "ORDER BY h.createdAt DESC")
    List<OrderHistoryEntity> findByOrderIdAndChangeTypeWithActorOrderByCreatedAtDesc(
            @Param("orderId") Long orderId,
            @Param("changeType") String changeType);

    /**
     * Tìm kiếm có lọc – dùng cho unified history API.
     * Tất cả tham số đều nullable (không lọc nếu null).
     *
     * <p>Dùng {@code LEFT JOIN FETCH} cho {@code changedByUser} để tránh N+1 khi map DTO.
     * {@code userInfo} được load lazy trong transaction của caller.
     *
     * @param actorRoleCode  Lọc theo role code của người thực hiện (vd: "EMPLOYEE"),
     *                       null = không lọc theo role.
     */
    @Query("SELECT h FROM OrderHistoryEntity h " +
           "LEFT JOIN FETCH h.changedByUser u " +
           "LEFT JOIN u.role r " +
           "WHERE (:orderId         IS NULL OR h.order.id              = :orderId) " +
           "  AND (:actorUserId     IS NULL OR u.id                    = :actorUserId) " +
           "  AND (:changeType      IS NULL OR h.changeType            = :changeType) " +
           "  AND (:from            IS NULL OR h.createdAt            >= :from) " +
           "  AND (:to              IS NULL OR h.createdAt            <= :to) " +
           "  AND (:actorRoleCode   IS NULL OR r.code                  = :actorRoleCode) " +
           "ORDER BY h.createdAt DESC")
    List<OrderHistoryEntity> search(
            @Param("orderId")       Long   orderId,
            @Param("actorUserId")   Long   actorUserId,
            @Param("changeType")    String changeType,
            @Param("from")          Date   from,
            @Param("to")            Date   to,
            @Param("actorRoleCode") String actorRoleCode);
}
