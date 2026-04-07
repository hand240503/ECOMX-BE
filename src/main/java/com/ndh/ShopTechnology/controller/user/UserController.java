package com.ndh.ShopTechnology.controller.user;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.dto.request.user.ChangeContactRequest;
import com.ndh.ShopTechnology.dto.request.user.ChangePasswordRequest;
import com.ndh.ShopTechnology.dto.request.user.ModUserInfoRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.services.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/users")
public class UserController {

        private final UserService userService;

        @Autowired
        public UserController(UserService userService) {
                this.userService = userService;
        }

        @GetMapping("/profile")
        public ResponseEntity<APIResponse<UserResponse>> getUserProfile() {
                UserResponse entity = userService.getProfile();

                if (entity == null) {
                        APIResponse<UserResponse> response = APIResponse.of(
                                        false,
                                        MessageConstant.USER_NOT_FOUND,
                                        null,
                                        List.of(ErrorResponse.builder()
                                                        .field("user")
                                                        .message(MessageConstant.USER_NOT_FOUND)
                                                        .build()),
                                        null);
                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(response);
                }

                APIResponse<UserResponse> response = APIResponse.of(
                                true,
                                MessageConstant.USER_INFO_RETRIEVED,
                                entity,
                                null,
                                null);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(response);
        }

        @PostMapping(value = "/profile", consumes = "multipart/form-data")
        public ResponseEntity<APIResponse<UserResponse>> updateUserProfile(
                        @RequestPart(value = "profile", required = false) ModUserInfoRequest request,
                        @RequestPart(value = "file", required = false) MultipartFile file) {

                if (file != null && !file.isEmpty()) {
                        userService.uploadAvatar(file);
                        if (request != null && request.getAvatar() != null) {
                                request = ModUserInfoRequest.builder()
                                                .fullName(request.getFullName())
                                                .telephone(request.getTelephone())
                                                .avatar(null)
                                                .managerId(request.getManagerId())
                                                .info01(request.getInfo01())
                                                .info02(request.getInfo02())
                                                .info03(request.getInfo03())
                                                .info04(request.getInfo04())
                                                .build();
                        }
                }

                UserResponse updatedUser = userService.updateProfileInfo(request);

                if (updatedUser == null) {
                        APIResponse<UserResponse> response = APIResponse.of(
                                        false,
                                        MessageConstant.USER_PROFILE_UPDATE_FAILED,
                                        null,
                                        List.of(ErrorResponse.builder()
                                                        .field("profile")
                                                        .message(MessageConstant.USER_PROFILE_UPDATE_FAILED)
                                                        .build()),
                                        null);
                        return ResponseEntity
                                        .status(HttpStatus.BAD_REQUEST)
                                        .body(response);
                }

                APIResponse<UserResponse> response = APIResponse.of(
                                true,
                                MessageConstant.USER_PROFILE_UPDATE_SUCCESS,
                                updatedUser,
                                null,
                                null);

                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(response);
        }

        @PostMapping("/profile/password")
        public ResponseEntity<APIResponse<Void>> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
                userService.changePassword(request);

                APIResponse<Void> response = APIResponse.of(
                                true,
                                "Đổi mật khẩu thành công",
                                null,
                                null,
                                null);

                return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        @PostMapping("/profile/contact")
        public ResponseEntity<APIResponse<LoginResponse>> changeContact(
                        @RequestBody @Valid ChangeContactRequest request) {
                LoginResponse updated = userService.changeContact(request);

                APIResponse<LoginResponse> response = APIResponse.of(
                                true,
                                "Cập nhật email/số điện thoại thành công",
                                updated,
                                null,
                                null);

                return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        // Avatar is updated via POST /profile with multipart/form-data
}