package com.ndh.ShopTechnology.dto.response.recommendation.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Hồ sơ sở thích 1 user (user_preference_profile) đã bung JSON metadata thành danh sách signal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private Long userId;
    private String userName;
    private String updatedAt;
    private List<Signal> signals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Signal {
        /** Loại signal: category / sub_category / brand / tag / price ... */
        private String type;
        /** Giá trị (tên category/brand/tag...). */
        private String key;
        /** Điểm số tương ứng. */
        private Double score;
    }
}
