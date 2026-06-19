package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

    Optional<ProductVariantEntity> findFirstByProduct_IdAndActiveTrueOrderBySortOrderAscIdAsc(Long productId);

    @Query("""
            SELECT DISTINCT v FROM ProductVariant v
            JOIN FETCH v.product p
            LEFT JOIN FETCH v.prices pr
            LEFT JOIN FETCH pr.unit
            WHERE v.id IN :ids
            """)
    List<ProductVariantEntity> findAllWithProductAndPricesByIdIn(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT DISTINCT v FROM ProductVariant v
            JOIN FETCH v.product p
            LEFT JOIN FETCH v.prices pr
            LEFT JOIN FETCH pr.unit
            WHERE v.id = :id
            """)
    Optional<ProductVariantEntity> findWithProductAndPricesById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT v FROM ProductVariant v
            JOIN FETCH v.product p
            WHERE p.id IN :productIds AND v.active = true
            ORDER BY p.id ASC, v.sortOrder ASC, v.id ASC
            """)
    List<ProductVariantEntity> findActiveByProductIdIn(@Param("productIds") Collection<Long> productIds);

    long countByProduct_Id(Long productId);

    boolean existsByIdAndProduct_Id(Long id, Long productId);

    // ===================== Tồn kho (atomic, chống bán âm) =====================

    /** Đọc biến thể với khóa ghi (pessimistic) để cập nhật tồn nhất quán. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariantEntity> findByIdForUpdate(@Param("id") Long id);

    /** Đọc onHand mới từ DB (projection scalar — phản ánh đúng sau bulk UPDATE). */
    @Query("SELECT v.onHand FROM ProductVariant v WHERE v.id = :id")
    Integer fetchOnHand(@Param("id") Long id);

    /** Đọc available = onHand - reserved mới từ DB (scalar). */
    @Query("SELECT (v.onHand - v.reserved) FROM ProductVariant v WHERE v.id = :id")
    Integer fetchAvailable(@Param("id") Long id);

    /** Đọc reserved mới từ DB (scalar). */
    @Query("SELECT v.reserved FROM ProductVariant v WHERE v.id = :id")
    Integer fetchReserved(@Param("id") Long id);

    /** Giữ hàng: chỉ tăng reserved khi còn đủ hàng bán (available >= qty). 1 = OK, 0 = không đủ. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProductVariant v
               SET v.reserved = v.reserved + :qty
             WHERE v.id = :id
               AND (v.onHand - v.reserved) >= :qty
            """)
    int reserveStock(@Param("id") Long id, @Param("qty") int qty);

    /** Giữ hàng vô điều kiện (cho đơn ĐÃ thanh toán, chấp nhận oversell). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProductVariant v
               SET v.reserved = v.reserved + :qty
             WHERE v.id = :id
            """)
    int forceReserve(@Param("id") Long id, @Param("qty") int qty);

    /** Nhả hàng đã giữ (không cho reserved âm). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProductVariant v
               SET v.reserved = v.reserved - :qty
             WHERE v.id = :id
               AND v.reserved >= :qty
            """)
    int releaseStock(@Param("id") Long id, @Param("qty") int qty);

    /** Xuất kho khi hoàn thành: trừ cả onHand lẫn reserved. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProductVariant v
               SET v.onHand = v.onHand - :qty,
                   v.reserved = v.reserved - :qty
             WHERE v.id = :id
               AND v.onHand >= :qty
               AND v.reserved >= :qty
            """)
    int commitSaleStock(@Param("id") Long id, @Param("qty") int qty);

    /** Nhập lại kho (hoàn hàng tốt) hoặc nhập kho mới: tăng onHand. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProductVariant v
               SET v.onHand = v.onHand + :qty
             WHERE v.id = :id
            """)
    int addOnHand(@Param("id") Long id, @Param("qty") int qty);

    /** Đặt thẳng onHand về một giá trị (kiểm kê). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProductVariant v
               SET v.onHand = :value
             WHERE v.id = :id
            """)
    int setOnHand(@Param("id") Long id, @Param("value") int value);
}
