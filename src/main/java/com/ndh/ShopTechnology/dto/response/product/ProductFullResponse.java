package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.product.PolicyEntity;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.Hibernate;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DTO thống nhất cho mọi API trả về sản phẩm (home, tìm kiếm, gợi ý, theo category, …).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductFullResponse {

  private Long id;
  private String productName;
  private String description;
  private Integer status;
  private Boolean isFeatured;
  private Long soldCount;
  private String tag;
  private Date createdDate;
  private Date modifiedDate;

  private BrandSummaryResponse brand;
  private CategorySummaryResponse category;
  private List<ProductPriceResponse> prices;
  /** Có dữ liệu khi entity đã fetch policies (vd. GET /products/{id}, GET /products/{id}/detail). */
  private List<PolicyResponse> policies;

  /** Chỉ có ý nghĩa với API recommendation — các API khác để null. */
  private Double recommendationScore;
  /** Chỉ có ý nghĩa với API recommendation — các API khác để null. */
  private String recommendationSource;

  private Double averageRating;
  private Long ratingCount;

  public static ProductFullResponse fromEntity(ProductEntity entity) {
    return fromEntity(entity, null, null, null, null);
  }

  public static ProductFullResponse fromEntity(
      ProductEntity entity,
      Double recommendationScore,
      String recommendationSource) {
    return fromEntity(entity, recommendationScore, recommendationSource, null, null);
  }

  public static ProductFullResponse fromEntity(
      ProductEntity entity,
      Double recommendationScore,
      String recommendationSource,
      Double averageRating,
      Long ratingCount) {
    if (entity == null) {
      return null;
    }
    List<ProductPriceResponse> priceList = null;
    if (entity.getPrices() != null && !entity.getPrices().isEmpty()) {
      priceList = entity.getPrices().stream()
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(PriceEntity::getId, Comparator.nullsLast(Long::compareTo)))
          .map(ProductPriceResponse::fromEntity)
          .collect(Collectors.toList());
    }
    List<PolicyResponse> policyList = null;
    if (Hibernate.isInitialized(entity.getPolicies()) && entity.getPolicies() != null) {
      policyList = entity.getPolicies().stream()
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(PolicyEntity::getId, Comparator.nullsLast(Long::compareTo)))
          .map(PolicyResponse::fromEntity)
          .collect(Collectors.toList());
    }
    return ProductFullResponse.builder()
        .id(entity.getId())
        .productName(entity.getProductName())
        .description(entity.getDescription())
        .status(entity.getStatus())
        .isFeatured(entity.getIsFeatured())
        .soldCount(entity.getSoldCount())
        .tag(entity.getTag())
        .createdDate(entity.getCreatedDate())
        .modifiedDate(entity.getModifiedDate())
        .brand(BrandSummaryResponse.fromEntity(entity.getBrand()))
        .category(CategorySummaryResponse.fromEntity(entity.getCategory()))
        .prices(priceList)
        .policies(policyList)
        .recommendationScore(recommendationScore)
        .recommendationSource(recommendationSource)
        .averageRating(averageRating)
        .ratingCount(ratingCount)
        .build();
  }
}
