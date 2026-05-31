package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.PaginationRequest;
import com.ndh.ShopTechnology.dto.request.user.AdminModUserInfoRequest;
import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.PaginationMetadata;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.services.permission.PermissionService;
import com.ndh.ShopTechnology.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

    private final UserService userService;
    private final PermissionService permissionService;

    @GetMapping("")
    public ResponseEntity<APIResponse<List<UserResponse>>> getStaffUsers(
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "10", required = false) int size) {
        permissionService.requireAnyPermission(PermissionCode.READ_USER);

        PaginationRequest request = new PaginationRequest();
        request.setPage(page);
        request.setSize(size);

        Page<UserResponse> userPage = userService.getStaffUsers(request);
        List<UserResponse> users = userPage.getContent();
        PaginationMetadata metadata = PaginationMetadata.fromPage(userPage);

        if (users.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.<List<UserResponse>>builder()
                            .success(false)
                            .message("No staff found")
                            .build());
        }

        return ResponseEntity.ok(
                APIResponse.<List<UserResponse>>builder()
                        .success(true)
                        .message("Staff retrieved successfully")
                        .data(users)
                        .metadata(metadata)
                        .build());
    }

    @PostMapping("")
    public ResponseEntity<APIResponse<UserResponse>> createStaffUser(@RequestBody CreateUserRequest request) {
        permissionService.requireAnyPermission(
                PermissionCode.CREATE_USER,
                PermissionCode.CREATE_ALL);

        UserResponse userResponse = userService.createStaffUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Staff user created successfully", userResponse, null, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<UserResponse>> getStaffUser(@PathVariable Long id) {
        permissionService.requireAnyPermission(PermissionCode.READ_USER);

        UserResponse userResponse = userService.getStaffUser(id);

        return ResponseEntity.ok(
                APIResponse.of(true, "Staff user retrieved successfully", userResponse, null, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<UserResponse>> updateStaffUser(
            @PathVariable Long id,
            @RequestBody AdminModUserInfoRequest request) {
        permissionService.requireAnyPermission(
                PermissionCode.UPDATE_USER,
                PermissionCode.UPDATE_ALL);

        request.setId(id);
        UserResponse userResponse = userService.updateStaffUser(request);

        return ResponseEntity.ok(
                APIResponse.of(true, "Staff user updated successfully", userResponse, null, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Map<String, Long>>> deleteStaffUser(@PathVariable Long id) {
        permissionService.requireAnyPermission(
                PermissionCode.DELETE_USER,
                PermissionCode.DELETE_ALL);

        userService.deleteStaffUser(id);
        return ResponseEntity.ok(
                APIResponse.of(true, "Staff user deleted successfully", Map.of("id", id), null, null));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<APIResponse<UserResponse>> resetStaffPassword(@PathVariable Long id) {
        permissionService.requireAnyPermission(
                PermissionCode.UPDATE_USER,
                PermissionCode.UPDATE_ALL);

        UserResponse userResponse = userService.resetStaffPassword(id);
        return ResponseEntity.ok(
                APIResponse.of(true, "Password reset; share temporary password with staff", userResponse, null, null));
    }
}
