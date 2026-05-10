package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.services.recommendation.dto.SessionContext;

public interface SessionContextService {
  /**
   * Build context từ collector_log của user trong N phút gần nhất
   * (chỉ event details, moreDetails, buy).
   * Trả về SessionContext rỗng nếu user không có hoạt động.
   */
  SessionContext buildContext(Long userId);

  /**
   * User đã đăng nhập ({@code userId > 0}): context theo user như {@link #buildContext(Long)}.
   * Khách hoặc thiếu user: context theo {@code sessionId} (bản ghi {@code user_id IS NULL}).
   */
  SessionContext buildContext(Long userId, String sessionId);
}