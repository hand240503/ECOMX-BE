package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.store.StoreStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreStockRepository extends JpaRepository<StoreStockEntity, Long> {

    Optional<StoreStockEntity> findByStore_IdAndVariant_Id(Long storeId, Long variantId);

    boolean existsByStore_IdAndVariant_Id(Long storeId, Long variantId);

    void deleteByStore_Id(Long storeId);

    boolean existsByStore_IdAndOnHandGreaterThan(Long storeId, Integer threshold);

    /**
     * Với một tập biến thể, trả về (storeId, số biến thể CÒN BÁN ĐƯỢC tại kho đó).
     * Kho chứa đủ tất cả biến thể nếu count == số biến thể yêu cầu.
     */
    @Query("""
            SELECT ss.store.id AS storeId, COUNT(DISTINCT ss.variant.id) AS cnt
            FROM store_stock ss
            WHERE ss.variant.id IN :variantIds
              AND (ss.onHand - ss.reserved) > 0
            GROUP BY ss.store.id
            """)
    List<Object[]> countAvailableVariantsPerStore(@Param("variantIds") List<Long> variantIds);

    /** Tồn của tất cả biến thể trong một kho; lọc theo tên SP / SKU (q null = tất cả). */
    @Query("""
            SELECT ss FROM store_stock ss
            JOIN FETCH ss.variant v
            JOIN FETCH v.product p
            WHERE ss.store.id = :storeId
              AND (:q IS NULL
                   OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(v.skuCode) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.id ASC, v.sortOrder ASC, v.id ASC
            """)
    List<StoreStockEntity> listByStore(@Param("storeId") Long storeId, @Param("q") String q);

    /** Tồn của một biến thể tại tất cả các kho. */
    @Query("""
            SELECT ss FROM store_stock ss
            JOIN FETCH ss.store s
            JOIN FETCH ss.variant v
            JOIN FETCH v.product p
            WHERE ss.variant.id = :variantId
            ORDER BY s.id ASC
            """)
    List<StoreStockEntity> listByVariant(@Param("variantId") Long variantId);

    // ===================== Scalar projections =====================

    @Query("SELECT ss.onHand FROM store_stock ss WHERE ss.store.id = :storeId AND ss.variant.id = :variantId")
    Integer fetchOnHand(@Param("storeId") Long storeId, @Param("variantId") Long variantId);

    @Query("SELECT (ss.onHand - ss.reserved) FROM store_stock ss WHERE ss.store.id = :storeId AND ss.variant.id = :variantId")
    Integer fetchAvailable(@Param("storeId") Long storeId, @Param("variantId") Long variantId);

    @Query("SELECT ss.reserved FROM store_stock ss WHERE ss.store.id = :storeId AND ss.variant.id = :variantId")
    Integer fetchReserved(@Param("storeId") Long storeId, @Param("variantId") Long variantId);

    // ===================== Cập nhật tồn (atomic, chống bán âm) =====================

    /** Giữ hàng: chỉ tăng reserved khi còn đủ bán (available >= qty). 1 = OK, 0 = không đủ. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE store_stock ss
               SET ss.reserved = ss.reserved + :qty
             WHERE ss.store.id = :storeId AND ss.variant.id = :variantId
               AND (ss.onHand - ss.reserved) >= :qty
            """)
    int reserveStock(@Param("storeId") Long storeId, @Param("variantId") Long variantId, @Param("qty") int qty);

    /** Giữ hàng vô điều kiện (đơn ĐÃ thanh toán, chấp nhận oversell). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE store_stock ss
               SET ss.reserved = ss.reserved + :qty
             WHERE ss.store.id = :storeId AND ss.variant.id = :variantId
            """)
    int forceReserve(@Param("storeId") Long storeId, @Param("variantId") Long variantId, @Param("qty") int qty);

    /** Nhả hàng đã giữ (không cho reserved âm). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE store_stock ss
               SET ss.reserved = ss.reserved - :qty
             WHERE ss.store.id = :storeId AND ss.variant.id = :variantId
               AND ss.reserved >= :qty
            """)
    int releaseStock(@Param("storeId") Long storeId, @Param("variantId") Long variantId, @Param("qty") int qty);

    /** Xuất kho khi hoàn thành: trừ cả onHand lẫn reserved. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE store_stock ss
               SET ss.onHand = ss.onHand - :qty,
                   ss.reserved = ss.reserved - :qty
             WHERE ss.store.id = :storeId AND ss.variant.id = :variantId
               AND ss.onHand >= :qty AND ss.reserved >= :qty
            """)
    int commitSaleStock(@Param("storeId") Long storeId, @Param("variantId") Long variantId, @Param("qty") int qty);

    /** Tăng onHand (nhập kho / nhập lại / nhận chuyển kho). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE store_stock ss
               SET ss.onHand = ss.onHand + :qty
             WHERE ss.store.id = :storeId AND ss.variant.id = :variantId
            """)
    int addOnHand(@Param("storeId") Long storeId, @Param("variantId") Long variantId, @Param("qty") int qty);

    /** Giảm onHand chỉ khi đủ tồn bán được (available >= qty) — dùng cho xuất chuyển kho. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE store_stock ss
               SET ss.onHand = ss.onHand - :qty
             WHERE ss.store.id = :storeId AND ss.variant.id = :variantId
               AND (ss.onHand - ss.reserved) >= :qty
            """)
    int reduceOnHandIfAvailable(@Param("storeId") Long storeId, @Param("variantId") Long variantId, @Param("qty") int qty);

    /** Đặt thẳng onHand về một giá trị (kiểm kê). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE store_stock ss
               SET ss.onHand = :value
             WHERE ss.store.id = :storeId AND ss.variant.id = :variantId
            """)
    int setOnHand(@Param("storeId") Long storeId, @Param("variantId") Long variantId, @Param("value") int value);
}
