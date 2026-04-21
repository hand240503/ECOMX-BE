package com.ndh.ShopTechnology.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;

/**
 * Pagination for product search, with optional spelling / query hints for the search UI.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSearchPaginationMetadata extends PaginationMetadata {

  private String suggestedQuery;
  private String spellSuggestion;

  public static ProductSearchPaginationMetadata fromPage(Page<?> page, String suggestion) {
    PaginationMetadata base = PaginationMetadata.fromPage(page);
    ProductSearchPaginationMetadata meta = new ProductSearchPaginationMetadata();
    meta.setPage(base.getPage());
    meta.setSize(base.getSize());
    meta.setTotalElements(base.getTotalElements());
    meta.setTotalPages(base.getTotalPages());
    meta.setFirst(base.isFirst());
    meta.setLast(base.isLast());
    meta.setHasNext(base.isHasNext());
    meta.setHasPrevious(base.isHasPrevious());
    meta.setNumberOfElements(base.getNumberOfElements());
    if (suggestion != null && !suggestion.isBlank()) {
      meta.setSuggestedQuery(suggestion);
      meta.setSpellSuggestion(suggestion);
    }
    return meta;
  }
}
