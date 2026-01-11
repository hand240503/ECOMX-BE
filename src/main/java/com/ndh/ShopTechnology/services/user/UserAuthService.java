package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.RegisterUserRequest;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;

public interface UserAuthService {

    /**
     * Register new user
     */
    LoginResponse registerUser(RegisterUserRequest request);

    /**
     * Login user
     */
    LoginResponse login(LoginRequest request); // ✅ Bỏ throws Exception

    /**
     * Refresh access token using refresh token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * Logout user and revoke tokens
     */
    void logout(String username);
}