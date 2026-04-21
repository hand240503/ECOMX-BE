package com.ndh.ShopTechnology.services.search;

import com.ndh.ShopTechnology.dto.response.search.TrendingKeywordResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Curated trending search keywords for the empty search screen (max 12 items for the storefront).
 */
@Service
public class SearchTrendingService {

  private static final List<TrendingKeywordResponse> DEFAULT_TRENDING = List.of(
      TrendingKeywordResponse.builder().keyword("Laptop").hot(true).build(),
      TrendingKeywordResponse.builder().keyword("Điện thoại").hot(true).build(),
      TrendingKeywordResponse.builder().keyword("Tai nghe").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Bàn phím cơ").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Chuột gaming").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Màn hình").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Ổ cứng SSD").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Loa Bluetooth").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Router WiFi").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Webcam").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Pin dự phòng").hot(false).build(),
      TrendingKeywordResponse.builder().keyword("Phụ kiện laptop").hot(false).build());

  public List<TrendingKeywordResponse> getTrendingKeywords() {
    return DEFAULT_TRENDING;
  }
}
