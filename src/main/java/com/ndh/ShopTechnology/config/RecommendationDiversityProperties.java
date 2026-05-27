package com.ndh.ShopTechnology.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "recommendation.diversity")
public class RecommendationDiversityProperties {

    private int maxPerBrand = 5;
}
