package com.ndh.ShopTechnology.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Đa dạng hoá gợi ý home: giới hạn số món tối đa cùng một {@code brand_id} trong một lượt xếp hạng.
 * {@code maxPerBrand} &lt;= 0: không áp giới hạn (giữ thứ tự blend như cũ).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "recommendation.diversity")
public class RecommendationDiversityProperties {

    /** Mặc định 5; 0 hoặc âm = không cap theo hãng. */
    private int maxPerBrand = 5;
}
