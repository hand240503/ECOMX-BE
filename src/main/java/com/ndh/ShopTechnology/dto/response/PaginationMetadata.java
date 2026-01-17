package com.ndh.ShopTechnology.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMetadata {
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean first;
  private boolean last;
  private boolean hasNext;
  private boolean hasPrevious;
  private int numberOfElements;

  public static PaginationMetadata fromPage(Page<?> page) {
    return PaginationMetadata.builder()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .hasNext(page.hasNext())
        .hasPrevious(page.hasPrevious())
        .numberOfElements(page.getNumberOfElements())
        .build();
  }
}
