package com.ndh.ShopTechnology.services.auth;

import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.RefreshTokenRequest;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;

public interface AuthService {

    /** Login cho trang User (FE) — chỉ chấp nhận tài khoản có role CUSTOMER. */
    LoginResponse login(LoginRequest request);

    /** Login cho trang Admin — chỉ chấp nhận tài khoản KHÔNG phải CUSTOMER. */
    LoginResponse adminLogin(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent);

    void logout(String username);
}
