package com.ndh.ShopTechnology.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(
                "similarItemsCf",      // key = productId  → similar (CF)
                "similarItemsContent", // key = productId  → similar (content)
                "userContentRecs",     // key = userId     → top-K từ cb_content_recommendation
                "popularItems",        // key = "all"      → top popular
                "longtermScores",
                "fbtCache"
        );
        mgr.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .recordStats());
        return mgr;
    }
}