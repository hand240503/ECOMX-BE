package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.entities.product.PolicyEntity;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import com.ndh.ShopTechnology.utils.ProductCatalogListing;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductFullResponse {

  private Long id;
  private Long sku;
  private String productName;
  private String description;

  @JsonProperty("l_description")
  private String lDescription;

  private Integer status;
  private Boolean isFeatured;
  private Boolean hotSale;
  private Long soldCount;
  private String tag;
  private Date createdDate;
  private Date modifiedDate;

  private BrandSummaryResponse brand;
  private CategorySummaryResponse category;
  private List<ProductPriceResponse> prices;
  @JsonProperty("from_effective_unit_price")
  private Double fromEffectiveUnitPrice;
  private List<ProductVariantResponse> variants;
  private List<PolicyResponse> policies;

  @Deprecated
  @JsonProperty("volume_price_tiers")
  private List<VolumePriceTierResponse> volumePriceTiers;

  @JsonProperty("purchase_with_purchase_programs")
  private List<ProductPurchaseWithPurchaseProgramResponse> purchaseWithPurchasePrograms;

  private Double recommendationScore;
  private String recommendationSource;

  private Double averageRating;
  private Long ratingCount;

  private String thumbnailUrl;
  private String mainImageUrl;
  private List<String> imageUrls;
  private List<ProductDocumentSummary> documents;

  @JsonProperty(value = "imageUrl", access = JsonProperty.Access.READ_ONLY)
  public String getImageUrl() {
    return mainImageUrl;
  }

  @JsonProperty(value = "coverImageUrl", access = JsonProperty.Access.READ_ONLY)
  public String getCoverImageUrl() {
    return mainImageUrl;
  }

  public static ProductFullResponse fromEntity(ProductEntity entity) {
    return fromEntity(entity, null, null, null, null, null);
  }

  public static ProductFullResponse fromEntity(
      ProductEntity entity,
      Double recommendationScore,
      String recommendationSource) {
    return fromEntity(entity, recommendationScore, recommendationSource, null, null, null);
  }

  public static ProductFullResponse fromEntity(
      ProductEntity entity,
      Double recommendationScore,
      String recommendationSource,
      Double averageRating,
      Long ratingCount) {
    return fromEntity(entity, recommendationScore, recommendationSource, averageRating, ratingCount, null);
  }

  public static ProductFullResponse fromEntity(
      ProductEntity entity,
      Double recommendationScore,
      String recommendationSource,
      Double averageRating,
      Long ratingCount,
      Map<Long, Double> variantDisplayUnitPrices) {
    if (entity == null) {
      return null;
    }
    List<ProductPriceResponse> priceList = null;
    com.ndh.ShopTechnology.entities.product.ProductVariantEntity listing =
        ProductCatalogListing.pickCheapestActiveVariant(entity, variantDisplayUnitPrices);
    if (listing != null && listing.getPrices() != null && !listing.getPrices().isEmpty()) {
      priceList = listing.getPrices().stream()
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(PriceEntity::getId, Comparator.nullsLast(Long::compareTo)))
          .map(ProductPriceResponse::fromEntity)
          .collect(Collectors.toList());
    }
    Double fromEffective = null;
    if (variantDisplayUnitPrices != null && entity.getVariants() != null) {
      fromEffective = entity.getVariants().stream()
          .filter(Objects::nonNull)
          .filter(v -> Boolean.TRUE.equals(v.getActive()) && v.getId() != null)
          .map(v -> variantDisplayUnitPrices.get(v.getId()))
          .filter(Objects::nonNull)
          .min(Double::compareTo)
          .orElse(null);
    }
    List<ProductVariantResponse> variantList = null;
    if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
      variantList = entity.getVariants().stream()
          .filter(Objects::nonNull)
          .sorted(Comparator
              .comparing(com.ndh.ShopTechnology.entities.product.ProductVariantEntity::getSortOrder,
                  Comparator.nullsLast(Integer::compareTo))
              .thenComparing(com.ndh.ShopTechnology.entities.product.ProductVariantEntity::getId,
                  Comparator.nullsLast(Long::compareTo)))
          .map(v -> ProductVariantResponse.fromEntity(
              v,
              variantDisplayUnitPrices != null && v.getId() != null
                  ? variantDisplayUnitPrices.get(v.getId())
                  : null))
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
        .sku(entity.getSku())
        .productName(entity.getProductName())
        .description(entity.getDescription())
        .lDescription(entity.getLDescription())
        .status(entity.getStatus())
        .isFeatured(entity.getIsFeatured())
        .hotSale(entity.getHotSale())
        .soldCount(entity.getSoldCount())
        .tag(entity.getTag())
        .createdDate(entity.getCreatedDate())
        .modifiedDate(entity.getModifiedDate())
        .brand(BrandSummaryResponse.fromEntity(entity.getBrand(), null))
        .category(CategorySummaryResponse.fromEntity(entity.getCategory()))
        .prices(priceList)
        .fromEffectiveUnitPrice(fromEffective)
        .variants(variantList)
        .policies(policyList)
        .recommendationScore(recommendationScore)
        .recommendationSource(recommendationSource)
        .averageRating(averageRating)
        .ratingCount(ratingCount)
        .build();
  }
}
