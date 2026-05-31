package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.log.PriceEventHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PriceEventHistoryRepository extends JpaRepository<PriceEventHistoryEntity, Long> {

    /** Lấy toàn bộ lịch sử của 1 chương trình cụ thể */
    List<PriceEventHistoryEntity> findByProgramTypeAndProgramIdOrderByCreatedAtDesc(
            String programType, Long programId);

    /** Lấy lịch sử theo product */
    List<PriceEventHistoryEntity> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Tìm kiếm có lọc — dùng cho trang admin lịch sử chương trình giá.
     * Tất cả tham số đều optional (null = bỏ qua điều kiện).
     */
    @Query(value = """
            SELECT h FROM PriceEventHistoryEntity h
            LEFT JOIN FETCH h.actorUser u
            WHERE (:programType IS NULL OR h.programType = :programType)
              AND (:programId   IS NULL OR h.programId   = :programId)
              AND (:eventType   IS NULL OR h.eventType   = :eventType)
              AND (:productId   IS NULL OR h.productId   = :productId)
              AND (:from        IS NULL OR h.createdAt  >= :from)
              AND (:to          IS NULL OR h.createdAt  <= :to)
            ORDER BY h.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(h) FROM PriceEventHistoryEntity h
            LEFT JOIN h.actorUser u
            WHERE (:programType IS NULL OR h.programType = :programType)
              AND (:programId   IS NULL OR h.programId   = :programId)
              AND (:eventType   IS NULL OR h.eventType   = :eventType)
              AND (:productId   IS NULL OR h.productId   = :productId)
              AND (:from        IS NULL OR h.createdAt  >= :from)
              AND (:to          IS NULL OR h.createdAt  <= :to)
            """)
    Page<PriceEventHistoryEntity> search(
            @Param("programType") String programType,
            @Param("programId")   Long programId,
            @Param("eventType")   String eventType,
            @Param("productId")   Long productId,
            @Param("from")        Date from,
            @Param("to")          Date to,
            Pageable pageable);
}
