package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.dto.request.PaginationRequest;
import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
import com.ndh.ShopTechnology.dto.request.user.ModUserInfoRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.PaginationMetadata;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

  private final UserService userService;

  @GetMapping("")
  public ResponseEntity<APIResponse<List<UserResponse>>> getAllUser(
      @RequestParam(value = "page", defaultValue = "0", required = false) int page,
      @RequestParam(value = "size", defaultValue = "10", required = false) int size) {

    PaginationRequest request = new PaginationRequest();
    request.setPage(page);
    request.setSize(size);

    Page<UserResponse> userPage = userService.getAllUsers(request);
    List<UserResponse> users = userPage.getContent();
    PaginationMetadata metadata = PaginationMetadata.fromPage(userPage);

    if (users.isEmpty()) {
      return ResponseEntity
          .status(HttpStatus.NOT_FOUND)
          .body(APIResponse.<List<UserResponse>>builder()
              .success(false)
              .message("No users found")
              .build());
    }

    return ResponseEntity.ok(
        APIResponse.<List<UserResponse>>builder()
            .success(true)
            .message("Users retrieved successfully")
            .data(users)
            .metadata(metadata)
            .build());
  }

  /**
   * Create new user
   * POST /api/v1/admin/users
   */
  @PostMapping("")
  public ResponseEntity<APIResponse<UserResponse>> createUser(@RequestBody CreateUserRequest request) {

    UserResponse userResponse = userService.createUser(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(APIResponse.of(true, "User created successfully", userResponse, null, null));
  }

  /**
   * Get user by ID
   * GET /api/v1/admin/users/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<APIResponse<UserResponse>> getUserInfo(@PathVariable Long id) {

    UserResponse userResponse = userService.getUserInfo(id);

    return ResponseEntity.ok(
        APIResponse.of(true, "User retrieved successfully", userResponse, null, null));
  }

  /**
   * Update user information
   * PUT /api/v1/admin/users
   */
  @PutMapping("")
  public ResponseEntity<APIResponse<UserResponse>> modUserInfo(@RequestBody ModUserInfoRequest request) {

    UserResponse userResponse = userService.updateUserInfo(request);

    return ResponseEntity.ok(
        APIResponse.of(true, "User updated successfully", userResponse, null, null));
  }
}