package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.PaginationRequest;
import com.ndh.ShopTechnology.dto.request.user.AdminModUserInfoRequest;
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
@RequestMapping("${api.prefix}/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final PermissionService permissionService;

    @GetMapping("")
    public ResponseEntity<APIResponse<List<UserResponse>>> listAllUsers(
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "10", required = false) int size) {
        permissionService.requireAnyPermission(PermissionCode.READ_USER, PermissionCode.READ_ALL);

        PaginationRequest request = new PaginationRequest();
        request.setPage(page);
        request.setSize(size);

        Page<UserResponse> userPage = userService.getAllUsersForAdmin(request);
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

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<UserResponse>> getUser(@PathVariable Long id) {
        permissionService.requireAnyPermission(PermissionCode.READ_USER, PermissionCode.READ_ALL);
        UserResponse userResponse = userService.getUserForAdmin(id);
        return ResponseEntity.ok(
                APIResponse.of(true, "User retrieved successfully", userResponse, null, null));
    }

    @PutMapping("")
    public ResponseEntity<APIResponse<UserResponse>> updateUser(@RequestBody AdminModUserInfoRequest request) {
        permissionService.requireAnyPermission(
                PermissionCode.UPDATE_USER,
                PermissionCode.UPDATE_ALL,
                PermissionCode.UPDATE_USER);
        UserResponse userResponse = userService.updateUserForAdmin(request);
        return ResponseEntity.ok(
                APIResponse.of(true, "User updated successfully", userResponse, null, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Map<String, Long>>> deleteUser(@PathVariable Long id) {
        permissionService.requireAnyPermission(
                PermissionCode.DELETE_USER,
                PermissionCode.DELETE_ALL);
        userService.deleteUserForAdmin(id);
        return ResponseEntity.ok(
                APIResponse.of(true, "User deleted successfully", Map.of("id", id), null, null));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<APIResponse<UserResponse>> resetPassword(@PathVariable Long id) {
        permissionService.requireAnyPermission(
                PermissionCode.UPDATE_USER,
                PermissionCode.UPDATE_ALL);
        UserResponse userResponse = userService.resetUserPasswordForAdmin(id);
        return ResponseEntity.ok(
                APIResponse.of(true, "Password reset; share temporary password with user", userResponse, null, null));
    }
}
