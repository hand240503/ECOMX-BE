package com.ndh.ShopTechnology.services.auth.impl;

import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.RefreshTokenRequest;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;
import com.ndh.ShopTechnology.services.auth.AuthService;
import com.ndh.ShopTechnology.services.user.UserAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAuthService userAuthService;

    @Override
    public LoginResponse login(LoginRequest request) {
        return userAuthService.login(request);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        return userAuthService.refreshToken(
                request.getRefreshToken(),
                request.getDeviceId(),
                ipAddress,
                userAgent
        );
    }

    @Override
    public void logout(String username) {
        userAuthService.logout(username);
    }
}
