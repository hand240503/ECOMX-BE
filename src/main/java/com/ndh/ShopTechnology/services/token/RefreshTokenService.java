package com.ndh.ShopTechnology.services.token;

import com.ndh.ShopTechnology.config.TokenProvider;
import com.ndh.ShopTechnology.entities.token.RefreshTokenEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.RefreshTokenRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    /**
     * Tạo refresh token mới cho user
     */
    @Transactional
    public RefreshTokenEntity createRefreshToken(String username) {
        // Load user
        UserEntity user = userRepository.findOneByUsername(username)
                .orElseThrow(() -> new CustomApiException(
                        HttpStatus.NOT_FOUND,
                        "User không tồn tại"
                ));

        // Revoke all old tokens của user này
        revokeUserTokens(user);

        // Generate new refresh token
        String tokenValue = tokenProvider.generateRefreshToken(username);

        // Calculate expiry date
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000);

        // Create entity
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(expiryDate)
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify và lấy refresh token từ database
     */
    @Transactional(readOnly = true)
    public RefreshTokenEntity verifyRefreshToken(String token) {
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token không hợp lệ"
                ));

        // Check if revoked
        if (refreshToken.getRevoked()) {
            throw new CustomApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token đã bị thu hồi"
            );
        }

        // Check if expired
        if (refreshToken.isExpired()) {
            throw new CustomApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token đã hết hạn"
            );
        }

        return refreshToken;
    }

    /**
     * Revoke all tokens của user
     */
    @Transactional
    public void revokeUserTokens(UserEntity user) {
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());
        log.info("Revoked all refresh tokens for user: {}", user.getUsername());
    }

    /**
     * Xóa các token đã hết hạn (có thể chạy scheduled)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired refresh tokens");
    }
}