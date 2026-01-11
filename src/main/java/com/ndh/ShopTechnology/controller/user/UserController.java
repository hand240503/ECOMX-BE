package com.ndh.ShopTechnology.controller.user;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.dto.request.user.ModUserInfoRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get user profile information", description = "Retrieve the profile information of the user")
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
                    null
            );
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        APIResponse<UserResponse> response = APIResponse.of(
                true,
                MessageConstant.USER_INFO_RETRIEVED,
                entity,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(summary = "Update user profile", description = "Modify the profile information of the current user.")
    @PutMapping("/profile")
    public ResponseEntity<APIResponse<UserResponse>> updateUserProfile(
            @RequestBody ModUserInfoRequest request) {

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
                    null
            );
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }

        APIResponse<UserResponse> response = APIResponse.of(
                true,
                MessageConstant.USER_PROFILE_UPDATE_SUCCESS,
                updatedUser,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}