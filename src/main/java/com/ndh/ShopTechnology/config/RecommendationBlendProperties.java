package com.ndh.ShopTechnology.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@ConfigurationProperties(prefix = "recommendation.blend")
public class RecommendationBlendProperties {

    private double longWeight = 0.6;
    private double shortWeight = 0.4;

    @PostConstruct
    void normalizeWeights() {
        double sum = longWeight + shortWeight;
        if (sum <= 0) {
            longWeight = 0.6;
            shortWeight = 0.4;
            return;
        }
        longWeight /= sum;
        shortWeight /= sum;
    }
}