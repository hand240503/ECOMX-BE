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

@RestController
@RequestMapping("${api.prefix}/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final UserService userService;
    private final PermissionService permissionService;

    @GetMapping("")
    public ResponseEntity<APIResponse<List<UserResponse>>> getEmployees(
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "10", required = false) int size) {
        permissionService.requireAnyPermission(PermissionCode.READ_USER);

        PaginationRequest request = new PaginationRequest();
        request.setPage(page);
        request.setSize(size);

        Page<UserResponse> userPage = userService.getEmployeeUsers(request);
        List<UserResponse> users = userPage.getContent();
        PaginationMetadata metadata = PaginationMetadata.fromPage(userPage);

        if (users.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.<List<UserResponse>>builder()
                            .success(false)
                            .message("No employees found")
                            .build());
        }

        return ResponseEntity.ok(
                APIResponse.<List<UserResponse>>builder()
                        .success(true)
                        .message("Employees retrieved successfully")
                        .data(users)
                        .metadata(metadata)
                        .build());
    }

    @PostMapping("")
    public ResponseEntity<APIResponse<UserResponse>> createEmployee(@RequestBody CreateUserRequest request) {
        permissionService.requireAnyPermission(
                PermissionCode.CREATE_USER,
                PermissionCode.CREATE_ALL);

        UserResponse userResponse = userService.createEmployeeUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Employee created successfully", userResponse, null, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<UserResponse>> getEmployee(@PathVariable Long id) {
        permissionService.requireAnyPermission(PermissionCode.READ_USER);

        UserResponse userResponse = userService.getEmployeeUser(id);

        return ResponseEntity.ok(
                APIResponse.of(true, "Employee retrieved successfully", userResponse, null, null));
    }

    @PutMapping("")
    public ResponseEntity<APIResponse<UserResponse>> updateEmployee(
            @RequestBody AdminModUserInfoRequest request) {
        permissionService.requireAnyPermission(
                PermissionCode.UPDATE_ALL,
                PermissionCode.UPDATE_USER);

        UserResponse userResponse = userService.updateEmployeeUser(request);

        return ResponseEntity.ok(
                APIResponse.of(true, "Employee updated successfully", userResponse, null, null));
    }
}
