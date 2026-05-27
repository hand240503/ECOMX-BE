package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.services.recommendation.dto.SessionContext;

public interface SessionContextService {
  SessionContext buildContext(Long userId);

  SessionContext buildContext(Long userId, String sessionId);
}
