package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.CbRecommendationDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.EventStatDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.ImplicitRatingDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.InsightsPage;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.InsightsSummaryDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.SimilarityPairDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.UserProfileDto;
import com.ndh.ShopTechnology.services.recommendation.RecommendationInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API admin: hiển thị kết quả pipeline gợi ý (recsys_builder) để trình bày/báo cáo.
 * Gồm: tổng quan, độ tương đồng item-item, implicit ratings, hồ sơ sở thích user,
 * và gợi ý theo profile (cb_content_recommendation).
 *
 * Tất cả chỉ ĐỌC, yêu cầu quyền READ_PRODUCT.
 */
@RestController
@RequestMapping("${api.prefix}/admin/recommendation-insights")
@RequiredArgsConstructor
public class AdminRecommendationInsightsController {

    private final RecommendationInsightsService service;

    @GetMapping("/summary")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<InsightsSummaryDto>> summary() {
        InsightsSummaryDto data = service.getSummary();
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy tổng quan gợi ý thành công", data, null, null));
    }

    @GetMapping("/similarities")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<java.util.List<SimilarityPairDto>>> similarities(
            @RequestParam(defaultValue = "cf_cosine") String algorithm,
            @RequestParam(required = false) Long source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        InsightsPage<SimilarityPairDto> p = service.getSimilarities(algorithm, source, page, size);
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy độ tương đồng thành công", p.getContent(), null,
                pageMeta(p.getPage(), p.getSize(), p.getTotal(), p.getTotalPages())));
    }

    @GetMapping("/implicit-ratings")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<java.util.List<ImplicitRatingDto>>> implicitRatings(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        InsightsPage<ImplicitRatingDto> p = service.getImplicitRatings(userId, productId, page, size);
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy implicit ratings thành công", p.getContent(), null,
                pageMeta(p.getPage(), p.getSize(), p.getTotal(), p.getTotalPages())));
    }

    @GetMapping("/profiles")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<java.util.List<UserProfileDto>>> profiles(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        InsightsPage<UserProfileDto> p = service.getUserProfiles(userId, page, size);
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy hồ sơ sở thích thành công", p.getContent(), null,
                pageMeta(p.getPage(), p.getSize(), p.getTotal(), p.getTotalPages())));
    }

    @GetMapping("/cb")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<java.util.List<CbRecommendationDto>>> cb(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        InsightsPage<CbRecommendationDto> p = service.getCbRecommendations(userId, page, size);
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy gợi ý theo profile thành công", p.getContent(), null,
                pageMeta(p.getPage(), p.getSize(), p.getTotal(), p.getTotalPages())));
    }

    @GetMapping("/event-stats")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<java.util.List<EventStatDto>>> eventStats(
            @RequestParam(defaultValue = "30") Integer days) {
        java.util.List<EventStatDto> data = service.getEventStats(days);
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy thống kê tương tác thành công", data, null, Map.of("days", days)));
    }

    private Map<String, Object> pageMeta(int page, int size, long total, int totalPages) {
        return Map.of(
                "page", page,
                "size", size,
                "total", total,
                "totalPages", totalPages);
    }
}
