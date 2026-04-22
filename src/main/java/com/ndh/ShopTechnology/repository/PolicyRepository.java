package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.PolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<PolicyEntity, Long> {

  Optional<PolicyEntity> findByCode(String code);

  List<PolicyEntity> findAllByActiveTrue();
}
