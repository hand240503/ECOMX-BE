package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.role.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(String code);

    Optional<RoleEntity> findByName(String name);

    boolean existsByCode(String code);
}