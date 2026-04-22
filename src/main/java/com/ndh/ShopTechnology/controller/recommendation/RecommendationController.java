package com.ndh.ShopTechnology.controller.recommendation;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.request.recommendation.SessionProfileRequest;
import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;
import com.ndh.ShopTechnology.services.recommendation.HybridRecommendationService;
import com.ndh.ShopTechnology.services.recommendation.RecommendationEnrichmentService;
import com.ndh.ShopTechnology.services.recommendation.SessionBasedRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final HybridRecommendationService hybridService;
    private final RecommendationEnrichmentService enrichmentService;
    private final SessionBasedRecommendationService sessionBasedService;

    @GetMapping("/home")
    public List<ProductFullResponse> home(
            @RequestParam Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        List<RecommendationItem> items =
                hybridService.getHomeRecommendations(userId, sessionId, offset, limit);
        return enrichmentService.enrich(items);
    }

    @GetMapping("/pdp/{productId}")
    public List<ProductFullResponse> pdp(
            @PathVariable Integer productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "10") int limit) {

        List<RecommendationItem> items = hybridService.getSimilarToProduct(productId, userId, sessionId, limit);
        return enrichmentService.enrich(items);
    }

    /**
     * PDP / listing: tương tự 1 sản phẩm theo <strong>hybrid có trọng số</strong> CF + content
     * (item–item), không cần session/recent. Khác {@code /pdp/...} (cascade CF rồi mới bổ sung content).
     */
    @GetMapping("/item-hybrid/{productId}")
    public List<ProductFullResponse> itemHybrid(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "10") int limit) {

        List<RecommendationItem> items = hybridService.getItemHybridSimilar(productId, limit);
        return enrichmentService.enrich(items);
    }

    @GetMapping("/post-purchase/{productId}")
    public List<ProductFullResponse> postPurchase(
            @PathVariable Integer productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "10") int limit) {

        List<RecommendationItem> items = hybridService.getPostPurchaseRecommendations(
                productId, userId, sessionId, limit);
        return enrichmentService.enrich(items);
    }

    @PostMapping("/session")
    public List<ProductFullResponse> session(
            @Valid @RequestBody SessionProfileRequest request) {
        return sessionBasedService.recommendForSession(request);
    }
}
