package com.ndh.ShopTechnology.dto.search;

import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import org.springframework.data.domain.Page;

public record ProductSearchResult(Page<ProductFullResponse> page, String suggestedQuery) {
}
