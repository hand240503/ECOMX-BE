package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.order.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {

    List<PaymentMethodEntity> findByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<PaymentMethodEntity> findByCode(String code);
}
