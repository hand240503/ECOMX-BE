package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.enums.UserState;

public interface UserStateService {
    UserState classify(Long userId);

    long countInteractions(Long userId);

    /**
     * Phân loại user dựa trên số lượng tương tác trong collector_log.
     *
     * @param userId ID user (có thể null cho guest)
     * @return UserState.NEW (0 events), COLD (1-9), ACTIVE (>=10)
     */
    UserState getUserState(Long userId);
}