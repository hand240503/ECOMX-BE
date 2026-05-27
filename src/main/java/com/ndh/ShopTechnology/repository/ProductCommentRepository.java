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

    Optional<ProductCommentEntity> findByUser_IdAndProduct_Id(Long userId, Long productId);

    List<ProductCommentEntity> findByProduct_IdAndIsHiddenFalseOrderByCreatedDateDesc(Long productId);

    List<ProductCommentEntity> findByProduct_IdOrderByCreatedDateDesc(Long productId);

    List<ProductCommentEntity> findByUser_IdOrderByCreatedDateDesc(Long userId);

    long countByProduct_IdAndIsHiddenFalse(Long productId);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    @Query("SELECT c FROM ProductCommentEntity c ORDER BY c.createdDate DESC")
    List<ProductCommentEntity> findAllOrderByCreatedDateDesc();
}
