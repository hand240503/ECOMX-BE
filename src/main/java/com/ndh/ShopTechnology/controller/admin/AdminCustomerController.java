package com.ndh.ShopTechnology.controller.admin;



import com.ndh.ShopTechnology.constants.PermissionCode;

import com.ndh.ShopTechnology.dto.request.PaginationRequest;

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



/**

 * Đọc danh sách và chi tiết tài khoản khách hàng (role {@code CUSTOMER}).

 */

@RestController

@RequestMapping("${api.prefix}/admin/customers")

@RequiredArgsConstructor

public class AdminCustomerController {



    private final UserService userService;

    private final PermissionService permissionService;



    @GetMapping("")

    public ResponseEntity<APIResponse<List<UserResponse>>> listCustomers(

            @RequestParam(value = "page", defaultValue = "0", required = false) int page,

            @RequestParam(value = "size", defaultValue = "10", required = false) int size) {

        permissionService.requireAnyPermission(

                PermissionCode.READ_USER,

                PermissionCode.READ_ALL);



        PaginationRequest request = new PaginationRequest();

        request.setPage(page);

        request.setSize(size);



        Page<UserResponse> userPage = userService.getCustomerUsers(request);

        List<UserResponse> users = userPage.getContent();

        PaginationMetadata metadata = PaginationMetadata.fromPage(userPage);



        if (users.isEmpty()) {

            return ResponseEntity

                    .status(HttpStatus.NOT_FOUND)

                    .body(APIResponse.<List<UserResponse>>builder()

                            .success(false)

                            .message("No customers found")

                            .build());

        }



        return ResponseEntity.ok(

                APIResponse.<List<UserResponse>>builder()

                        .success(true)

                        .message("Customers retrieved successfully")

                        .data(users)

                        .metadata(metadata)

                        .build());

    }



    @GetMapping("/{id}")

    public ResponseEntity<APIResponse<UserResponse>> getCustomer(@PathVariable Long id) {

        permissionService.requireAnyPermission(

                PermissionCode.READ_USER,

                PermissionCode.READ_ALL);



        UserResponse userResponse = userService.getCustomerUser(id);

        return ResponseEntity.ok(

                APIResponse.of(true, "Customer retrieved successfully", userResponse, null, null));

    }

}


