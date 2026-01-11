package com.ndh.ShopTechnology.controller.auth;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.RefreshTokenRequest;
import com.ndh.ShopTechnology.dto.request.auth.RegisterUserRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;
import com.ndh.ShopTechnology.services.user.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    /**
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<APIResponse<LoginResponse>> registerUser(
            @RequestBody @Valid RegisterUserRequest request) {

        LoginResponse loginResponse = userAuthService.registerUser(request);

        APIResponse<LoginResponse> response = APIResponse.of(
                true,
                MessageConstant.USER_REGISTER_SUCCESS,
                loginResponse,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<APIResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request) {

        LoginResponse loginResponse = userAuthService.login(request);

        APIResponse<LoginResponse> response = APIResponse.of(
                true,
                MessageConstant.LOGIN_SUCCESS,
                loginResponse,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * POST /api/v1/auth/refresh
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<APIResponse<LoginResponse>> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request) {

        LoginResponse loginResponse = userAuthService.refreshToken(request.getRefreshToken());

        APIResponse<LoginResponse> response = APIResponse.of(
                true,
                "Token refreshed successfully",
                loginResponse,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * POST /api/v1/auth/logout
     * Logout and revoke all refresh tokens
     */
    @PostMapping("/logout")
    public ResponseEntity<APIResponse<Void>> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        userAuthService.logout(username);

        APIResponse<Void> response = APIResponse.of(
                true,
                "Đăng xuất thành công",
                null,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}