package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.enums.UserState;

public interface UserStateService {
    UserState classify(Long userId);

    long countInteractions(Long userId);

    UserState getUserState(Long userId);
}
