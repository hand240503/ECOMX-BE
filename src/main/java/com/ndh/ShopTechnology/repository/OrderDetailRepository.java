package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetailEntity, Long> {

    @EntityGraph(attributePaths = "product")
    List<OrderDetailEntity> findByOrder_IdOrderByIdAsc(Long orderId);
}
