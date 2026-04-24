package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.order.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = "paymentMethod")
    List<OrderEntity> findByUser_IdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = "paymentMethod")
    List<OrderEntity> findByUser_IdAndStatusOrderByIdDesc(Long userId, Integer status);

    @EntityGraph(attributePaths = "paymentMethod")
    Optional<OrderEntity> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = "paymentMethod")
    Optional<OrderEntity> findByCheckoutSessionPublicIdAndUser_Id(
            String checkoutSessionPublicId, Long userId);

    @EntityGraph(attributePaths = {"paymentMethod", "user"})
    Optional<OrderEntity> findByVnpayCheckoutTxnRef(Long vnpayCheckoutTxnRef);
}
