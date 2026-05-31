package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.dto.request.auth.ForgotPasswordRequest;
import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.ResetPasswordByTokenRequest;
import com.ndh.ShopTechnology.dto.request.auth.RegisterUserRequest;
import com.ndh.ShopTechnology.dto.request.auth.VerifyForgotPasswordOTPRequest;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;

public interface UserAuthService {

    LoginResponse registerUser(RegisterUserRequest request);

    /** Login trang User — chỉ chấp nhận CUSTOMER. */
    LoginResponse login(LoginRequest request);

    /** Login trang Admin — chỉ chấp nhận non-CUSTOMER. */
    LoginResponse adminLogin(LoginRequest request);

    LoginResponse refreshToken(String refreshToken, String deviceId, String ipAddress, String userAgent);

    void logout(String username);

    void requestForgotPassword(ForgotPasswordRequest request);

    boolean verifyForgotPasswordOtpAndSendResetLink(VerifyForgotPasswordOTPRequest request);

    void resetPasswordByToken(ResetPasswordByTokenRequest request);
}
