//package com.ndh.ShopTechnology.controller.admin;
//
//import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
//import com.ndh.ShopTechnology.dto.request.user.ModUserInfoRequest;
//import com.ndh.ShopTechnology.dto.response.APIResponse;
//import com.ndh.ShopTechnology.dto.response.user.UserResponse;
//import com.ndh.ShopTechnology.services.user.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("${api.prefix}/admin/users")
//@RequiredArgsConstructor
//public class AdminUserController {
//
//    private final UserService userService;
//
////    @GetMapping("")
////    public ResponseEntity<APIResponse<ResultPagination>> getAllUser(
////            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
////            @RequestParam(value = "size", defaultValue = "10", required = false) int size) {
////
////        ResultPagination resultPagination = userService.getAllUsers(page, size);
////
////        if (resultPagination.getLst().isEmpty()) {
////            return ResponseEntity
////                    .status(HttpStatus.NOT_FOUND)
////                    .body(APIResponse.of(false, "No users found", null, null, null));
////        }
////
////        return ResponseEntity.ok(
////                APIResponse.of(true, "Users retrieved successfully", resultPagination, null, null)
////        );
////    }
//
////    @PostMapping("")
////    public ResponseEntity<APIResponse<ResultPagination>> getAllUser(@RequestBody PaginationRequest request) {
////
////        ResultPagination resultPagination = userService.getAllUsers(request);
////
////        if (resultPagination.getLst().isEmpty()) {
////            return ResponseEntity
////                    .status(HttpStatus.NOT_FOUND)
////                    .body(APIResponse.of(false, "No users found", null, null, null));
////        }
////
////        return ResponseEntity.ok(
////                APIResponse.of(true, "Users retrieved successfully", resultPagination, null, null)
////        );
////    }
//
//    /**
//     * Create new user
//     * POST /api/v1/admin/users
//     */
//    @PostMapping("")
//    public ResponseEntity<APIResponse<UserResponse>> createUser(@RequestBody CreateUserRequest request) {
//
//        UserResponse userResponse = userService.createUser(request);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(APIResponse.of(true, "User created successfully", userResponse, null, null));
//    }
//
//    /**
//     * Get user by ID
//     * GET /api/v1/admin/users/{id}
//     */
//    @GetMapping("/{id}")
//    public ResponseEntity<APIResponse<UserResponse>> getUserInfo(@PathVariable Long id) {
//
//        UserResponse userResponse = userService.getUserInfo(id);
//
//        return ResponseEntity.ok(
//                APIResponse.of(true, "User retrieved successfully", userResponse, null, null)
//        );
//    }
//
//    /**
//     * Update user information
//     * PUT /api/v1/admin/users
//     */
//    @PutMapping("")
//    public ResponseEntity<APIResponse<UserResponse>> modUserInfo(@RequestBody ModUserInfoRequest request) {
//
//        UserResponse userResponse = userService.updateUserInfo(request);
//
//        return ResponseEntity.ok(
//                APIResponse.of(true, "User updated successfully", userResponse, null, null)
//        );
//    }
//}