package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.dto.request.auth.ForgotPasswordRequest;
import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.ResetPasswordByTokenRequest;
import com.ndh.ShopTechnology.dto.request.auth.RegisterUserRequest;
import com.ndh.ShopTechnology.dto.request.auth.VerifyForgotPasswordOTPRequest;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;

public interface UserAuthService {

    /**
     * Register new user
     */
    LoginResponse registerUser(RegisterUserRequest request);

    /**
     * Login user
     */
    LoginResponse login(LoginRequest request);

    /**
     * Refresh access token using refresh token
     */
    LoginResponse refreshToken(String refreshToken, String deviceId, String ipAddress, String userAgent);

    /**
     * Logout user and revoke tokens
     */
    void logout(String username);

    void requestForgotPassword(ForgotPasswordRequest request);

    boolean verifyForgotPasswordOtpAndSendResetLink(VerifyForgotPasswordOTPRequest request);

    void resetPasswordByToken(ResetPasswordByTokenRequest request);
}