package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.services.recommendation.dto.SessionContext;

public interface SessionContextService {
  /**
   * Build context từ collector_log của user trong N phút gần nhất.
   * Trả về SessionContext rỗng nếu user không có hoạt động.
   */
  SessionContext buildContext(Long userId);
}