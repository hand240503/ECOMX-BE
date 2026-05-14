package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findOneByUsername(String username);

    Optional<UserEntity> findOneByEmail(String email);

    Optional<UserEntity> findOneByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    /**
     * Load user kèm role (permission_codes JSON trên {@code roles}), user permission grants, userInfo và default address.
     * Dùng cho login và authentication.
     */
    @Query("SELECT DISTINCT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH u.userPermissions " +
            "LEFT JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.addresses addr " +
            "WHERE u.username = :username " +
            "AND (addr.isDefault = true OR addr.id IS NULL)")
    Optional<UserEntity> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    /**
     * Load user kèm role (permission_codes JSON), user permission grants, userInfo và địa chỉ.
     * Đồng bộ với login về độ đầy đủ (roles, permissions, defaultAddress); fetch toàn bộ địa chỉ
     * để trường hợp chưa có địa chỉ mặc định vẫn trả về user (khác truy vấn login tối ưu theo username).
     */
    @Query("SELECT DISTINCT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH u.userPermissions " +
            "LEFT JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.addresses " +
            "WHERE u.id = :id")
    Optional<UserEntity> findByIdWithRolesAndPermissions(@Param("id") Long id);

    @Query("SELECT u.username FROM UserEntity u WHERE u.id = :id")
    Optional<String> findUsernameById(@Param("id") Long id);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.userInfo WHERE u.id = :id")
    Optional<UserEntity> findByIdWithInfo(@Param("id") Long id);

    @EntityGraph(attributePaths = {"role", "userPermissions", "userInfo", "addresses"})
    @Query("SELECT u FROM UserEntity u WHERE u.role IS NOT NULL AND u.role.code <> :customerCode")
    Page<UserEntity> findPagedNonCustomerUsers(@Param("customerCode") String customerCode, Pageable pageable);

    @EntityGraph(attributePaths = {"role", "userPermissions", "userInfo", "addresses"})
    Page<UserEntity> findByRole_Code(String roleCode, Pageable pageable);
}
