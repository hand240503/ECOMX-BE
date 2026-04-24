package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.order.CheckoutSessionEntity;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckoutSessionRepository extends JpaRepository<CheckoutSessionEntity, Long> {

    @EntityGraph(attributePaths = {"user", "paymentMethod"})
    Optional<CheckoutSessionEntity> findByIdAndUser_IdAndStatus(
            Long id, Long userId, CheckoutSessionStatus status);

    @EntityGraph(attributePaths = {"user", "paymentMethod"})
    @Query("select c from CheckoutSessionEntity c where c.id = :id")
    Optional<CheckoutSessionEntity> findByIdWithUserAndPaymentMethod(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "paymentMethod"})
    Optional<CheckoutSessionEntity> findByPublicIdAndUser_Id(String publicId, Long userId);
}
