package com.ndh.ShopTechnology.controller.auth;

import com.ndh.ShopTechnology.constants.MessageConstant;
import com.ndh.ShopTechnology.dto.request.auth.ForgotPasswordRequest;
import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.RefreshTokenRequest;
import com.ndh.ShopTechnology.dto.request.auth.ResetPasswordByTokenRequest;
import com.ndh.ShopTechnology.dto.request.auth.RegisterUserRequest;
import com.ndh.ShopTechnology.dto.request.auth.VerifyForgotPasswordOTPRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;
import com.ndh.ShopTechnology.services.auth.AuthService;
import com.ndh.ShopTechnology.services.user.UserAuthService;
import jakarta.servlet.http.HttpServletRequest;
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
public class AuthController {

    private final AuthService authService;
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

        LoginResponse loginResponse = authService.login(request);

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
     * Access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<APIResponse<LoginResponse>> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request,
            HttpServletRequest httpServletRequest) {

        LoginResponse loginResponse = authService.refreshToken(
                request,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );

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
            if (authentication == null || authentication.getName() == null) {
                    APIResponse<Void> response = APIResponse.of(
                                    false,
                                    "Bạn chưa đăng nhập",
                                    null,
                                    null,
                                    null);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String username = authentication.getName();

            authService.logout(username);

            APIResponse<Void> response = APIResponse.of(
                            true,
                            "Đăng xuất thành công",
                            null,
                            null,
                            null);

            return ResponseEntity
                            .status(HttpStatus.OK)
                            .body(response);
    }

    @PostMapping("/password/forgot/request")
    public ResponseEntity<APIResponse<Void>> requestForgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {
        userAuthService.requestForgotPassword(request);

        APIResponse<Void> response = APIResponse.of(
                true,
                "Mã OTP đã được gửi đến email của bạn   ",
                null,
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/password/forgot/verify-otp")
    public ResponseEntity<APIResponse<Void>> verifyForgotPasswordOtp(
            @RequestBody @Valid VerifyForgotPasswordOTPRequest request) {
        boolean success = userAuthService.verifyForgotPasswordOtpAndSendResetLink(request);
        if (!success) {
            APIResponse<Void> response = APIResponse.of(
                    false,
                    "Mã xác thực không đúng hoặc đã hết hạn",
                    null,
                    null,
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        APIResponse<Void> response = APIResponse.of(
                true,
                "Xác thực OTP thành công. Liên kết đặt lại mật khẩu đã được gửi qua email",
                null,
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<APIResponse<Void>> resetPasswordByToken(
            @RequestBody @Valid ResetPasswordByTokenRequest request) {
        userAuthService.resetPasswordByToken(request);

        APIResponse<Void> response = APIResponse.of(
                true,
                "Đặt lại mật khẩu thành công",
                null,
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}