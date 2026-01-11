package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.dto.request.user.CreateAddressRequest;
import com.ndh.ShopTechnology.dto.request.user.UpdateAddressRequest;
import com.ndh.ShopTechnology.dto.response.user.UserAddressResponse;

import java.util.List;

public interface AddressService {
  UserAddressResponse createAddress(CreateAddressRequest request);

  List<UserAddressResponse> getAllAddresses();

  UserAddressResponse getAddressById(Long id);

  UserAddressResponse updateAddress(Long id, UpdateAddressRequest request);

  void deleteAddress(Long id);

  UserAddressResponse setDefaultAddress(Long id);
}
