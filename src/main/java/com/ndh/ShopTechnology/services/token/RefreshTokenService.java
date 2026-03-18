package com.ndh.ShopTechnology.services.token;

import com.ndh.ShopTechnology.config.TokenProvider;
import com.ndh.ShopTechnology.entities.token.RefreshTokenEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import io.jsonwebtoken.Claims;
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

    private static final String REASON_ROTATED = "ROTATED";
    private static final String REASON_REUSE_DETECTED = "REUSE_DETECTED";
    private static final String REASON_LOGOUT = "LOGOUT";
    private static final String REASON_LOGIN_REPLACED = "LOGIN_REPLACED";
    private static final String REASON_EXPIRED = "EXPIRED";

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    /**
     * Tạo refresh token root cho 1 phiên đăng nhập mới
     */
    @Transactional
    public RefreshTokenEntity createInitialRefreshToken(String username, String deviceId) {
        UserEntity user = userRepository.findOneByUsername(username)
                .orElseThrow(() -> new CustomApiException(
                        HttpStatus.NOT_FOUND,
                        "User không tồn tại"
                ));

        // Giữ hành vi "single session per user" như hiện tại
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now(), REASON_LOGIN_REPLACED);
        return issueRefreshToken(user, UUID.randomUUID().toString(), null, deviceId);
    }

    /**
     * Backward-compat method để không phá call site cũ
     */
    @Transactional
    public RefreshTokenEntity createRefreshToken(String username) {
        return createInitialRefreshToken(username, null);
    }

    /**
     * Rotate refresh token theo cơ chế token family + reuse detection
     */
    @Transactional
    public RefreshTokenEntity rotateRefreshToken(
            String rawRefreshToken,
            String deviceId,
            String ipAddress,
            String userAgent) {
        Claims claims;
        try {
            claims = tokenProvider.parseRefreshTokenClaims(rawRefreshToken);
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ");
        }

        String tokenId = claims.getId();
        String familyId = claims.get("family_id", String.class);

        if (tokenId == null || tokenId.isBlank() || familyId == null || familyId.isBlank()) {
            throw new CustomApiException(HttpStatus.UNAUTHORIZED, "Refresh token thiếu metadata bảo mật");
        }

        RefreshTokenEntity currentToken = refreshTokenRepository.findByTokenIdForUpdate(tokenId)
                .orElseThrow(() -> new CustomApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token không hợp lệ"
                ));

        // Chặn token bị tráo payload (jti hợp lệ nhưng token string mismatch)
        if (!rawRefreshToken.equals(currentToken.getToken())) {
            throw new CustomApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ");
        }

        if (currentToken.isExpired()) {
            LocalDateTime now = LocalDateTime.now();
            currentToken.setRevoked(true);
            currentToken.setRevokedAt(now);
            currentToken.setRevokedReason(REASON_EXPIRED);
            refreshTokenRepository.save(currentToken);

            throw new CustomApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token đã hết hạn"
            );
        }

        // Nếu token đã bị consume/revoke mà tiếp tục dùng => reuse detected
        if (Boolean.TRUE.equals(currentToken.getRevoked()) || currentToken.getUsedAt() != null) {
            handleTokenReuse(currentToken, ipAddress, userAgent);
            throw new CustomApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Phát hiện sử dụng lại refresh token. Vui lòng đăng nhập lại."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        currentToken.setUsedAt(now);
        currentToken.setRotatedAt(now);
        currentToken.setRevoked(true);
        currentToken.setRevokedAt(now);
        currentToken.setRevokedReason(REASON_ROTATED);

        String nextDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId : currentToken.getDeviceId();
        RefreshTokenEntity nextToken = issueRefreshToken(
                currentToken.getUser(),
                currentToken.getFamilyId(),
                currentToken.getTokenId(),
                nextDeviceId
        );

        currentToken.setReplacedByTokenId(nextToken.getTokenId());
        refreshTokenRepository.save(currentToken);

        log.info("Refresh token rotated: user={}, oldTokenId={}, newTokenId={}, familyId={}",
                currentToken.getUser().getUsername(), currentToken.getTokenId(), nextToken.getTokenId(), currentToken.getFamilyId());

        return nextToken;
    }

    /**
     * Revoke all tokens của user
     */
    @Transactional
    public void revokeUserTokens(UserEntity user) {
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now(), REASON_LOGOUT);
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

    @SuppressWarnings("null")
    private RefreshTokenEntity issueRefreshToken(
            UserEntity user,
            String familyId,
            String parentTokenId,
            String deviceId) {
        String tokenId = UUID.randomUUID().toString();
        String tokenValue = tokenProvider.generateRefreshToken(
                user.getUsername(),
                tokenId,
                familyId,
                parentTokenId,
                deviceId
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000);

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(tokenValue)
                .tokenId(tokenId)
                .familyId(familyId)
                .parentTokenId(parentTokenId)
                .deviceId(deviceId)
                .user(user)
                .expiryDate(expiryDate)
                .createdAt(now)
                .revoked(false)
                .build();

        RefreshTokenEntity savedToken = refreshTokenRepository.save(refreshToken);
        return savedToken;
    }

    private void handleTokenReuse(RefreshTokenEntity token, String ipAddress, String userAgent) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.revokeTokenFamily(token.getFamilyId(), now, REASON_REUSE_DETECTED);
        log.warn(
                "SECURITY_EVENT refresh_token_reuse_detected user={} tokenId={} familyId={} deviceId={} ip={} userAgent={}",
                token.getUser().getUsername(),
                token.getTokenId(),
                token.getFamilyId(),
                token.getDeviceId(),
                ipAddress,
                userAgent
        );
    }
}