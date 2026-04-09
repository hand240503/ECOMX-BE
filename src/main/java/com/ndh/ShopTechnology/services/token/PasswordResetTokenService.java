package com.ndh.ShopTechnology.services.token;

import com.ndh.ShopTechnology.entities.token.PasswordResetTokenEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final long RESET_TOKEN_EXPIRY_MINUTES = 15;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional
    public String issueResetToken(UserEntity user) {
        passwordResetTokenRepository.revokeAllActiveByUser(user);

        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        String tokenHash = sha256(rawToken);
        LocalDateTime now = LocalDateTime.now();

        PasswordResetTokenEntity tokenEntity = PasswordResetTokenEntity.builder()
                .user(user)
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiryAt(now.plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                .revoked(false)
                .build();

        passwordResetTokenRepository.save(Objects.requireNonNull(tokenEntity));
        return rawToken;
    }

    @Transactional
    public UserEntity consumeToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ");
        }

        String tokenHash = sha256(rawToken.trim());
        PasswordResetTokenEntity tokenEntity = passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new CustomApiException(
                        HttpStatus.BAD_REQUEST,
                        "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        if (Boolean.TRUE.equals(tokenEntity.getRevoked()) || tokenEntity.getUsedAt() != null || tokenEntity.isExpired()) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }

        tokenEntity.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(tokenEntity);
        return tokenEntity.getUser();
    }

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteExpiredOrInactive(LocalDateTime.now());
        log.info("Password reset tokens cleaned up");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
