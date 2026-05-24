package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.comment.ProductCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCommentRepository extends JpaRepository<ProductCommentEntity, Long> {

    /** Comment của một user trên một sản phẩm cụ thể. */
    Optional<ProductCommentEntity> findByUser_IdAndProduct_Id(Long userId, Long productId);

    /** Tất cả comment hiển thị (chưa bị ẩn) của một sản phẩm, sắp xếp mới nhất lên trước. */
    List<ProductCommentEntity> findByProduct_IdAndIsHiddenFalseOrderByCreatedDateDesc(Long productId);

    /** Tất cả comment (kể cả bị ẩn) của một sản phẩm — dành cho admin. */
    List<ProductCommentEntity> findByProduct_IdOrderByCreatedDateDesc(Long productId);

    /** Tất cả comment của một user. */
    List<ProductCommentEntity> findByUser_IdOrderByCreatedDateDesc(Long userId);

    /** Đếm số comment hiển thị của một sản phẩm. */
    long countByProduct_IdAndIsHiddenFalse(Long productId);

    /**
     * Kiểm tra user đã có comment trên sản phẩm này chưa.
     * Dùng trước khi cho phép tạo mới.
     */
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    /**
     * Tất cả comment của admin — phân trang thủ công (dùng findAll nếu cần paging).
     * Trả về cả comment bị ẩn, sắp xếp mới nhất lên trước.
     */
    @Query("SELECT c FROM ProductCommentEntity c ORDER BY c.createdDate DESC")
    List<ProductCommentEntity> findAllOrderByCreatedDateDesc();
}
