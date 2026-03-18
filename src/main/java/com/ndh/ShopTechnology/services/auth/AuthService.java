package com.ndh.ShopTechnology.services.auth;

import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.RefreshTokenRequest;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent);

    void logout(String username);
}
