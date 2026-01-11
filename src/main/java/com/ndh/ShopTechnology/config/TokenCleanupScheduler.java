package com.ndh.ShopTechnology.config;

import com.ndh.ShopTechnology.services.token.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    /**
     * Chạy mỗi ngày lúc 3h sáng để xóa expired tokens
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens");
        refreshTokenService.cleanupExpiredTokens();
        log.info("Completed cleanup of expired refresh tokens");
    }
}