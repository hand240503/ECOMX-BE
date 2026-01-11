package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.user.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddressEntity, Long> {

  List<UserAddressEntity> findByUserId(Long userId);

  Optional<UserAddressEntity> findByIdAndUserId(Long id, Long userId);

  @Query("SELECT a FROM user_address a WHERE a.user.id = :userId AND a.isDefault = true")
  Optional<UserAddressEntity> findDefaultAddressByUserId(@Param("userId") Long userId);

  void deleteByIdAndUserId(Long id, Long userId);
}
