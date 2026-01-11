package com.ndh.ShopTechnology.services.user.impl;

import com.ndh.ShopTechnology.dto.request.user.CreateAddressRequest;
import com.ndh.ShopTechnology.dto.request.user.UpdateAddressRequest;
import com.ndh.ShopTechnology.dto.response.user.UserAddressResponse;
import com.ndh.ShopTechnology.entities.user.UserAddressEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.UserAddressRepository;
import com.ndh.ShopTechnology.services.user.AddressService;
import com.ndh.ShopTechnology.services.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

  private final UserAddressRepository addressRepository;
  private final UserService userService;

  public AddressServiceImpl(UserAddressRepository addressRepository, UserService userService) {
    this.addressRepository = addressRepository;
    this.userService = userService;
  }

  @Override
  @Transactional
  public UserAddressResponse createAddress(CreateAddressRequest request) {
    UserEntity currentUser = userService.getCurrentUser();

    // If this is set as default, unset other default addresses
    if (Boolean.TRUE.equals(request.getIsDefault())) {
      addressRepository.findDefaultAddressByUserId(currentUser.getId())
          .ifPresent(addr -> {
            addr.setIsDefault(false);
            addressRepository.save(addr);
          });
    }

    UserAddressEntity address = UserAddressEntity.builder()
        .user(currentUser)
        .addressLine(request.getAddressLine())
        .city(request.getCity())
        .state(request.getState())
        .country(request.getCountry())
        .zipCode(request.getZipCode())
        .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
        .build();

    address = addressRepository.save(address);
    return UserAddressResponse.fromEntity(address);
  }

  @Override
  public List<UserAddressResponse> getAllAddresses() {
    UserEntity currentUser = userService.getCurrentUser();
    List<UserAddressEntity> addresses = addressRepository.findByUserId(currentUser.getId());
    return addresses.stream()
        .map(UserAddressResponse::fromEntity)
        .collect(Collectors.toList());
  }

  @Override
  public UserAddressResponse getAddressById(Long id) {
    UserEntity currentUser = userService.getCurrentUser();
    UserAddressEntity address = addressRepository.findByIdAndUserId(id, currentUser.getId())
        .orElseThrow(() -> new NotFoundEntityException("Address not found with id: " + id));
    return UserAddressResponse.fromEntity(address);
  }

  @Override
  @Transactional
  public UserAddressResponse updateAddress(Long id, UpdateAddressRequest request) {
    UserEntity currentUser = userService.getCurrentUser();
    UserAddressEntity address = addressRepository.findByIdAndUserId(id, currentUser.getId())
        .orElseThrow(() -> new NotFoundEntityException("Address not found with id: " + id));

    // Update fields if provided
    if (request.getAddressLine() != null) {
      address.setAddressLine(request.getAddressLine());
    }
    if (request.getCity() != null) {
      address.setCity(request.getCity());
    }
    if (request.getState() != null) {
      address.setState(request.getState());
    }
    if (request.getCountry() != null) {
      address.setCountry(request.getCountry());
    }
    if (request.getZipCode() != null) {
      address.setZipCode(request.getZipCode());
    }

    // Handle default address change
    if (request.getIsDefault() != null) {
      if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
        // Unset other default addresses
        addressRepository.findDefaultAddressByUserId(currentUser.getId())
            .ifPresent(addr -> {
              if (!addr.getId().equals(id)) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
              }
            });
        address.setIsDefault(true);
      } else if (Boolean.FALSE.equals(request.getIsDefault())) {
        address.setIsDefault(false);
      }
    }

    address = addressRepository.save(address);
    return UserAddressResponse.fromEntity(address);
  }

  @Override
  @Transactional
  public void deleteAddress(Long id) {
    UserEntity currentUser = userService.getCurrentUser();
    UserAddressEntity address = addressRepository.findByIdAndUserId(id, currentUser.getId())
        .orElseThrow(() -> new NotFoundEntityException("Address not found with id: " + id));
    addressRepository.delete(address);
  }

  @Override
  @Transactional
  public UserAddressResponse setDefaultAddress(Long id) {
    UserEntity currentUser = userService.getCurrentUser();
    UserAddressEntity address = addressRepository.findByIdAndUserId(id, currentUser.getId())
        .orElseThrow(() -> new NotFoundEntityException("Address not found with id: " + id));

    // Unset other default addresses
    addressRepository.findDefaultAddressByUserId(currentUser.getId())
        .ifPresent(addr -> {
          if (!addr.getId().equals(id)) {
            addr.setIsDefault(false);
            addressRepository.save(addr);
          }
        });

    address.setIsDefault(true);
    address = addressRepository.save(address);
    return UserAddressResponse.fromEntity(address);
  }
}
