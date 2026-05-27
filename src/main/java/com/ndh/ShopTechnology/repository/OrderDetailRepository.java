package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetailEntity, Long> {

    @EntityGraph(attributePaths = "product")
    List<OrderDetailEntity> findByOrder_IdOrderByIdAsc(Long orderId);

    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od " +
           "WHERE od.order.user.id = :userId " +
           "AND od.product.id = :productId " +
           "AND od.order.status = :status")
    boolean existsByOrderUserIdAndProductIdAndOrderStatus(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("status") Integer status);
}
