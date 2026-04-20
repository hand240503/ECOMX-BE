package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.log.CollectorLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface CollectorLogRepository extends JpaRepository<CollectorLogEntity, Long> {

        // ─────────────────────────────────────────────────────────────
        // Các method CRUD / filter cho CollectorLogService (có sẵn)
        // ─────────────────────────────────────────────────────────────

        List<CollectorLogEntity> findByUserId(Long userId);

        List<CollectorLogEntity> findByProductId(Long productId);

        List<CollectorLogEntity> findByEvent(String event);

        List<CollectorLogEntity> findBySessionId(String sessionId);

        @Query("""
                        SELECT c FROM CollectorLogEntity c
                        WHERE c.timestamp BETWEEN :startDate AND :endDate
                        ORDER BY c.timestamp DESC
                        """)
        List<CollectorLogEntity> findByTimestampBetween(
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate);

        @Query("""
                        SELECT c FROM CollectorLogEntity c
                        WHERE c.user.id = :userId
                          AND c.timestamp BETWEEN :startDate AND :endDate
                        ORDER BY c.timestamp DESC
                        """)
        List<CollectorLogEntity> findByUserIdAndTimestampBetween(
                        @Param("userId") Long userId,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate);

        @Query("""
                        SELECT c FROM CollectorLogEntity c
                        WHERE c.product.id = :productId
                          AND c.timestamp BETWEEN :startDate AND :endDate
                        ORDER BY c.timestamp DESC
                        """)
        List<CollectorLogEntity> findByProductIdAndTimestampBetween(
                        @Param("productId") Long productId,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate);

        // ─────────────────────────────────────────────────────────────
        // Các method cho Recommendation (Bước 4 + Bước 5)
        // ─────────────────────────────────────────────────────────────

        /** Đếm tổng số event của 1 user — dùng cho UserStateService (Bước 4). */
        @Query(value = """
                        SELECT COUNT(*) FROM collector_log WHERE user_id = :userId
                        """, nativeQuery = true)
        long countByUserId(@Param("userId") Long userId);

        /**
         * N product_id user đã tương tác gần nhất (Bước 5 - hybrid ACTIVE).
         * Truyền sessionId = null để không filter theo session.
         * Dùng GROUP BY + MAX(timestamp): MySQL không cho ORDER BY cột ngoài SELECT khi dùng DISTINCT.
         */
        @Query(value = """
                        SELECT product_id FROM (
                            SELECT product_id, MAX(`timestamp`) AS last_ts
                            FROM collector_log
                            WHERE user_id = :userId
                              AND product_id IS NOT NULL
                              AND (:sessionId IS NULL OR session_id = :sessionId)
                            GROUP BY product_id
                            ORDER BY last_ts DESC
                            LIMIT :limit
                        ) t
                        """, nativeQuery = true)
        List<Long> findRecentProductIdsByUser(
                        @Param("userId") Long userId,
                        @Param("sessionId") String sessionId,
                        @Param("limit") int limit);

        @Query(value = """
                        SELECT product_id FROM (
                            SELECT product_id, MAX(timestamp) AS last_ts
                            FROM collector_log
                            WHERE user_id = :userId
                              AND timestamp >= :since
                              AND product_id IS NOT NULL
                            GROUP BY product_id
                            ORDER BY last_ts DESC
                            LIMIT :limit
                        ) t
                        """, nativeQuery = true)
        List<Long> findRecentProductIdsByUserSince(
                        @Param("userId") Long userId,
                        @Param("since") LocalDateTime since,
                        @Param("limit") int limit);


    @Query(value = """
    SELECT product_id            AS productId,
           MAX(`timestamp`)      AS lastTs,
           COUNT(*)              AS cnt
    FROM collector_log
    WHERE user_id = :userId
      AND `timestamp` >= :since
      AND product_id IS NOT NULL
    GROUP BY product_id
    ORDER BY lastTs DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findRecentProductActivityByUser(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("limit") int limit
    );
}