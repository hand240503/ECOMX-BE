package com.ndh.ShopTechnology.controller.user;

import com.ndh.ShopTechnology.dto.request.user.CreateAddressRequest;
import com.ndh.ShopTechnology.dto.request.user.UpdateAddressRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.user.UserAddressResponse;
import com.ndh.ShopTechnology.services.user.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/users/addresses")
public class AddressController {

  private final AddressService addressService;

  @Autowired
  public AddressController(AddressService addressService) {
    this.addressService = addressService;
  }

  @PostMapping
  public ResponseEntity<APIResponse<UserAddressResponse>> createAddress(
      @Valid @RequestBody CreateAddressRequest request) {
    try {
      UserAddressResponse address = addressService.createAddress(request);
      APIResponse<UserAddressResponse> response = APIResponse.of(
          true,
          "Address created successfully",
          address,
          null,
          null);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      APIResponse<UserAddressResponse> response = APIResponse.of(
          false,
          "Failed to create address: " + e.getMessage(),
          null,
          List.of(ErrorResponse.builder()
              .field("address")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }

  @GetMapping
  public ResponseEntity<APIResponse<List<UserAddressResponse>>> getAllAddresses() {
    List<UserAddressResponse> addresses = addressService.getAllAddresses();
    APIResponse<List<UserAddressResponse>> response = APIResponse.of(
        true,
        "Addresses retrieved successfully",
        addresses,
        null,
        null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<APIResponse<UserAddressResponse>> getAddressById(@PathVariable Long id) {
    try {
      UserAddressResponse address = addressService.getAddressById(id);
      APIResponse<UserAddressResponse> response = APIResponse.of(
          true,
          "Address retrieved successfully",
          address,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<UserAddressResponse> response = APIResponse.of(
          false,
          "Address not found",
          null,
          List.of(ErrorResponse.builder()
              .field("id")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<APIResponse<UserAddressResponse>> updateAddress(
      @PathVariable Long id,
      @Valid @RequestBody UpdateAddressRequest request) {
    try {
      UserAddressResponse address = addressService.updateAddress(id, request);
      APIResponse<UserAddressResponse> response = APIResponse.of(
          true,
          "Address updated successfully",
          address,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<UserAddressResponse> response = APIResponse.of(
          false,
          "Failed to update address: " + e.getMessage(),
          null,
          List.of(ErrorResponse.builder()
              .field("address")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<APIResponse<Void>> deleteAddress(@PathVariable Long id) {
    try {
      addressService.deleteAddress(id);
      APIResponse<Void> response = APIResponse.of(
          true,
          "Address deleted successfully",
          null,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<Void> response = APIResponse.of(
          false,
          "Failed to delete address: " + e.getMessage(),
          null,
          List.of(ErrorResponse.builder()
              .field("id")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }

  @PutMapping("/{id}/default")
  public ResponseEntity<APIResponse<UserAddressResponse>> setDefaultAddress(@PathVariable Long id) {
    try {
      UserAddressResponse address = addressService.setDefaultAddress(id);
      APIResponse<UserAddressResponse> response = APIResponse.of(
          true,
          "Default address set successfully",
          address,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<UserAddressResponse> response = APIResponse.of(
          false,
          "Failed to set default address: " + e.getMessage(),
          null,
          List.of(ErrorResponse.builder()
              .field("id")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }
}
