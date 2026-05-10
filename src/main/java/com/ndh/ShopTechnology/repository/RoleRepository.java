package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.role.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho {@link RoleEntity}.
 *
 * <p>{@code permissionCodes} hiện được lưu trên cột JSON của chính bảng {@code roles}, nên không cần
 * {@code JOIN FETCH} riêng — mọi query mặc định đã load đầy đủ.
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(String code);

    Optional<RoleEntity> findByName(String name);

    boolean existsByCode(String code);
}
