package com.ndh.ShopTechnology.dto.search;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import org.springframework.data.domain.Page;

/**
 * Product search page plus optional alternate query hint (e.g. spelling).
 */
public record ProductSearchResult(Page<ProductFullResponse> page, String suggestedQuery) {
}
