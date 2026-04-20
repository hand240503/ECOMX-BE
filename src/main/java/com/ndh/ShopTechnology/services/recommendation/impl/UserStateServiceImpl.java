package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.enums.UserState;
import com.ndh.ShopTechnology.repository.CollectorLogRepository;
import com.ndh.ShopTechnology.services.recommendation.UserStateService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStateServiceImpl implements UserStateService {

    private static final int COLD_THRESHOLD = 1;
    private static final int ACTIVE_THRESHOLD = 10;

    private final CollectorLogRepository collectorLogRepository;

    @Override
    @Transactional(readOnly = true)
    public UserState classify(Long userId) {
        if (userId == null || userId <= 0)
            return UserState.NEW;
        long n = collectorLogRepository.countByUserId(userId);
        if (n < COLD_THRESHOLD)
            return UserState.NEW;
        if (n < ACTIVE_THRESHOLD)
            return UserState.COLD;
        return UserState.ACTIVE;
    }

    @Override
    @Transactional(readOnly = true)
    public long countInteractions(Long userId) {
        if (userId == null || userId <= 0)
            return 0;
        return collectorLogRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserState getUserState(Long userId) {
        if (userId == null || userId <= 0) {
            return UserState.NEW;
        }
        long count = collectorLogRepository.countByUserId(userId);
        log.debug("[UserState] userId={} eventCount={}", userId, count);
        if (count == 0)
            return UserState.NEW;
        if (count < ACTIVE_THRESHOLD)
            return UserState.COLD;
        return UserState.ACTIVE;
    }
}