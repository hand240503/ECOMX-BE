package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findOneByUsername(String username);

    Optional<UserEntity> findOneByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Load user với roles, permissions, userInfo và default address
     * Dùng cho login và authentication
     */
    @Query("SELECT DISTINCT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "LEFT JOIN FETCH u.userPermissions up " +
            "LEFT JOIN FETCH up.permission " +
            "LEFT JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.addresses addr " +
            "WHERE u.username = :username " +
            "AND (addr.isDefault = true OR addr.id IS NULL)")
    Optional<UserEntity> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    /**
     * Load user với userInfo (không cần roles/permissions)
     * Dùng cho các case chỉ cần thông tin cơ bản
     */
    @Query("SELECT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.userInfo " +
            "WHERE u.id = :id")
    Optional<UserEntity> findByIdWithInfo(@Param("id") Long id);
}