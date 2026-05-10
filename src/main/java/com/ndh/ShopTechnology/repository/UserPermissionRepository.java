package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.user.UserPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermissionEntity, Long> {

    List<UserPermissionEntity> findAllByUser_Id(Long userId);

    Optional<UserPermissionEntity> findByUser_IdAndPermissionCode(Long userId, Integer permissionCode);

    @Modifying
    @Query("DELETE FROM UserPermissionEntity up WHERE up.user.id = :userId AND up.permissionCode = :code")
    int deleteByUserIdAndPermissionCode(@Param("userId") Long userId, @Param("code") Integer code);

    @Modifying
    @Query("DELETE FROM UserPermissionEntity up WHERE up.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
