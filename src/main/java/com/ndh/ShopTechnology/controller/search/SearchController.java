package com.ndh.ShopTechnology.controller.search;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.search.TrendingKeywordResponse;
import com.ndh.ShopTechnology.services.search.SearchTrendingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/search")
public class SearchController {

  private final SearchTrendingService searchTrendingService;

  @Autowired
  public SearchController(SearchTrendingService searchTrendingService) {
    this.searchTrendingService = searchTrendingService;
  }

  @GetMapping("/trending")
  public ResponseEntity<APIResponse<List<TrendingKeywordResponse>>> trending() {
    List<TrendingKeywordResponse> keywords = searchTrendingService.getTrendingKeywords();
    APIResponse<List<TrendingKeywordResponse>> response = APIResponse.of(
        true,
        "Trending keywords retrieved successfully",
        keywords,
        null,
        null);
    return ResponseEntity.ok(response);
  }
}
