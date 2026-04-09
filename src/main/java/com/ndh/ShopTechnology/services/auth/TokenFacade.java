package com.ndh.ShopTechnology.services.auth;

import com.ndh.ShopTechnology.dto.response.user.LoginResponse;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.services.token.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenFacade {

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public LoginResponse issueLoginResponse(UserEntity user) {
        var newRefresh = refreshTokenService.createInitialRefreshToken(user.getUsername(), null);
        String newAccessToken = jwtService.generateAccessToken(user.getUsername());

        return LoginResponse.builder()
                .userInfo(UserResponse.fromEntity(user))
                .accessToken(newAccessToken)
                .refreshToken(newRefresh.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }
}
