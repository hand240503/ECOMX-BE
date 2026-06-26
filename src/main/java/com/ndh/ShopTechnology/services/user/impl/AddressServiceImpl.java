package com.ndh.ShopTechnology.services.user.impl;

import com.ndh.ShopTechnology.dto.delivery.GeocodedAddress;
import com.ndh.ShopTechnology.dto.delivery.RouteMetrics;
import com.ndh.ShopTechnology.dto.request.user.CreateAddressRequest;
import com.ndh.ShopTechnology.dto.request.user.UpdateAddressRequest;
import com.ndh.ShopTechnology.dto.response.user.UserAddressResponse;
import com.ndh.ShopTechnology.entities.user.AddressType;
import com.ndh.ShopTechnology.entities.user.UserAddressEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.entities.store.StoreEntity;
import com.ndh.ShopTechnology.repository.UserAddressRepository;
import com.ndh.ShopTechnology.services.delivery.DeliveryRoutingService;
import com.ndh.ShopTechnology.services.store.StoreService;
import com.ndh.ShopTechnology.services.user.AddressService;
import com.ndh.ShopTechnology.services.user.UserService;
import com.ndh.ShopTechnology.utils.ShippingFeeCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

  private final UserAddressRepository addressRepository;
  private final UserService userService;
  private final DeliveryRoutingService deliveryRoutingService;
  private final StoreService storeService;

  public AddressServiceImpl(
          UserAddressRepository addressRepository,
          UserService userService,
          DeliveryRoutingService deliveryRoutingService,
          StoreService storeService) {
    this.addressRepository = addressRepository;
    this.userService = userService;
    this.deliveryRoutingService = deliveryRoutingService;
    this.storeService = storeService;
  }

  @Override
  @Transactional
  public UserAddressResponse createAddress(CreateAddressRequest request) {
    UserEntity currentUser = userService.getCurrentUser();

    if (Boolean.TRUE.equals(request.getIsDefault())) {
      addressRepository
          .findDefaultAddressByUserId(currentUser.getId(), AddressType.USER)
          .ifPresent(addr -> {
            addr.setIsDefault(false);
            addressRepository.save(addr);
          });
    }

    UserAddressEntity address = UserAddressEntity.builder()
        .user(currentUser)
        .addressType(AddressType.USER)
        .addressLine(request.getAddressLine())
        .city(request.getCity())
        .state(request.getState())
        .country(request.getCountry())
        .zipCode(request.getZipCode())
        .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
        .build();

    enrichUserAddressGeo(address, request.getStoreId());
    address = addressRepository.save(address);
    return UserAddressResponse.fromEntity(address);
  }

  @Override
  public List<UserAddressResponse> getAllAddresses() {
    UserEntity currentUser = userService.getCurrentUser();
    List<UserAddressEntity> addresses =
        addressRepository.findByUserIdAndAddressType(currentUser.getId(), AddressType.USER);
    return addresses.stream()
        .map(UserAddressResponse::fromEntity)
        .collect(Collectors.toList());
  }

  @Override
  public UserAddressResponse getAddressById(Long id) {
    UserEntity currentUser = userService.getCurrentUser();
    UserAddressEntity address = addressRepository
        .findByIdAndUserIdAndAddressType(id, currentUser.getId(), AddressType.USER)
        .orElseThrow(() -> new NotFoundEntityException("Address not found with id: " + id));
    return UserAddressResponse.fromEntity(address);
  }

  @Override
  @Transactional
  public UserAddressResponse updateAddress(Long id, UpdateAddressRequest request) {
    UserEntity currentUser = userService.getCurrentUser();
    UserAddressEntity address = addressRepository
        .findByIdAndUserIdAndAddressType(id, currentUser.getId(), AddressType.USER)
        .orElseThrow(() -> new NotFoundEntityException("Address not found with id: " + id));

    boolean geoRelevantChanged = false;
    if (request.getAddressLine() != null) {
      address.setAddressLine(request.getAddressLine());
      geoRelevantChanged = true;
    }
    if (request.getCity() != null) {
      address.setCity(request.getCity());
      geoRelevantChanged = true;
    }
    if (request.getState() != null) {
      address.setState(request.getState());
      geoRelevantChanged = true;
    }
    if (request.getCountry() != null) {
      address.setCountry(request.getCountry());
      geoRelevantChanged = true;
    }
    if (request.getZipCode() != null) {
      address.setZipCode(request.getZipCode());
    }

    if (request.getIsDefault() != null) {
      if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
        addressRepository
            .findDefaultAddressByUserId(currentUser.getId(), AddressType.USER)
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

    // Tính lại phí nếu địa chỉ đổi HOẶC User đổi kho đang chọn (gửi storeId).
    if (geoRelevantChanged || request.getStoreId() != null) {
      enrichUserAddressGeo(address, request.getStoreId());
    }

    address = addressRepository.save(address);
    return UserAddressResponse.fromEntity(address);
  }

  @Override
  @Transactional
  public void deleteAddress(Long id) {
    UserEntity currentUser = userService.getCurrentUser();
    UserAddressEntity address = addressRepository
        .findByIdAndUserIdAndAddressType(id, currentUser.getId(), AddressType.USER)
        .orElseThrow(() -> new NotFoundEntityException("Address not found with id: " + id));
    addressRepository.delete(address);
  }

  @Override
  @Transactional
  public UserAddressResponse setDefaultAddress(Long id) {
    UserEntity currentUser = userService.getCurrentUser();
    UserAddressEntity address = addressRepository
        .findByIdAndUserIdAndAddressType(id, currentUser.getId(), AddressType.USER)
        .orElseThrow(() -> new NotFoundEntityException("Address not found with id: " + id));

    addressRepository
        .findDefaultAddressByUserId(currentUser.getId(), AddressType.USER)
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

  private void enrichUserAddressGeo(UserAddressEntity address, Long storeId) {
    if (address.getAddressType() != AddressType.USER) {
      return;
    }
    String query = buildGeocodeQuery(address);
    GeocodedAddress geo = deliveryRoutingService.geocodeAddress(query);
    address.setLatitude(geo.latitude());
    address.setLongitude(geo.longitude());
    // Điểm xuất phát tính phí ship = kho (store) User đang chọn; nếu không có → kho mặc định.
    double[] origin = resolveShippingOrigin(storeId);
    try {
      RouteMetrics route = deliveryRoutingService.routeDriving(
          geo.latitude(), geo.longitude(), origin[0], origin[1]);
      double meters = route.distanceMeters();
      address.setDistanceToWarehouseMeters(meters);
      address.setShippingFeeVnd(ShippingFeeCalculator.fromDistanceMeters(meters));
    } catch (CustomApiException e) {
      log.warn("OSRM failed for user address id={}: {}", address.getId(), e.getMessage());
      address.setDistanceToWarehouseMeters(null);
      address.setShippingFeeVnd(null);
    }
  }

  /**
   * Toạ độ kho dùng làm điểm xuất phát tính phí ship cho địa chỉ.
   * Ưu tiên kho do User chọn ({@code storeId}); nếu null hoặc kho thiếu toạ độ thì
   * dùng kho mặc định; cuối cùng fallback về kho cấu hình hệ thống (legacy).
   */
  private double[] resolveShippingOrigin(Long storeId) {
    StoreEntity store = null;
    if (storeId != null) {
      store = storeService.getEntityOrThrow(storeId);
    }
    if (store == null) {
      store = storeService.getDefaultStoreOrNull();
    }
    if (store != null && store.getLatitude() != null && store.getLongitude() != null) {
      return new double[] { store.getLatitude(), store.getLongitude() };
    }
    return deliveryRoutingService.resolveWarehouseLatLon();
  }

  private static String buildGeocodeQuery(UserAddressEntity e) {
    return Stream.of(e.getAddressLine(), e.getCity(), e.getState(), e.getCountry())
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(", "));
  }
}
