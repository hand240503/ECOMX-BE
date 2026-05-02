package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.user.AddressType;
import com.ndh.ShopTechnology.entities.user.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddressEntity, Long> {

  List<UserAddressEntity> findByUserIdAndAddressType(Long userId, AddressType addressType);

  Optional<UserAddressEntity> findByIdAndUserIdAndAddressType(Long id, Long userId, AddressType addressType);

  @Query("SELECT a FROM user_address a WHERE a.user.id = :userId AND a.isDefault = true AND a.addressType = :type")
  Optional<UserAddressEntity> findDefaultAddressByUserId(
          @Param("userId") Long userId, @Param("type") AddressType type);

  long countByAddressType(AddressType addressType);

  Optional<UserAddressEntity> findFirstByAddressType(AddressType addressType);

  void deleteByIdAndUserId(Long id, Long userId);
}
