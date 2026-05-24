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

/**
 * DTO thống nhất cho mọi API trả về sản phẩm (home, tìm kiếm, gợi ý, theo category, …).
 *
 * <p>JSON gồm {@code description}, {@code l_description} (mô tả dài) và các field khác map trực tiếp từ
 * {@link ProductEntity}.
 *
 * <p>Trường {@link #prices} là giá catalog (theo đơn vị) của biến thể đại diện — active và rẻ nhất
 * (ưu tiên {@link #fromEffectiveUnitPrice} / map giá hiển thị khi {@code ProductServiceImpl} truyền map vào overload đầy đủ của {@code fromEntity}).
 * Chi tiết từng SKU nằm trong {@link #variants}; mỗi variant có thêm {@code effective_unit_price}
 * (khi có price change hiệu lực thì trùng giá áp dụng PC) và {@code active_price_change}.
 *
 * <p>{@link #purchaseWithPurchasePrograms} gom chương trình PWP liên quan SPU. **Mix-and-match** hiển thị trên từng
 * phân loại: {@link ProductVariantResponse#getVolumePriceTiers()}.
 *
 * <p>Các URL ảnh ({@code thumbnailUrl}, {@code mainImageUrl}, {@code imageUrls}) và danh sách {@link #documents}
 * được gán từ bảng {@code document} trong service, không được gán trong {@link #fromEntity}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductFullResponse {

  private Long id;
  /** Mã SKU (số), có thể null. */
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
  /** Giá catalog theo đơn vị của biến thể đại diện (active, rẻ nhất — có thể theo giá hiển thị khi đã resolve). */
  private List<ProductPriceResponse> prices;
  /**
   * Min của {@code effective_unit_price} trong các biến thể đang {@code active} — tiện gắn nhãn “Từ …” trên card.
   */
  @JsonProperty("from_effective_unit_price")
  private Double fromEffectiveUnitPrice;
  /** Đầy đủ biến thể (SKU) kèm giá — dùng cho PDP / admin. */
  private List<ProductVariantResponse> variants;
  /** Có dữ liệu khi entity đã fetch policies (vd. GET /products/{id}, GET /products/{id}/detail). */
  private List<PolicyResponse> policies;

  /**
   * @deprecated Chuyển sang {@link ProductVariantResponse#getVolumePriceTiers()}. JSON thường không còn field này.
   */
  @Deprecated
  @JsonProperty("volume_price_tiers")
  private List<VolumePriceTierResponse> volumePriceTiers;

  /**
   * Chương trình PWP đang bật có liên quan đến SPU; {@code role} trong phần tử là {@code companion} hoặc {@code anchor}.
   */
  @JsonProperty("purchase_with_purchase_programs")
  private List<ProductPurchaseWithPurchaseProgramResponse> purchaseWithPurchasePrograms;

  /** Chỉ có ý nghĩa với API recommendation — các API khác để null. */
  private Double recommendationScore;
  /** Chỉ có ý nghĩa với API recommendation — các API khác để null. */
  private String recommendationSource;

  private Double averageRating;
  private Long ratingCount;

  /** Địa chỉ ảnh đại diện (thường trùng {@link #mainImageUrl}; document đầu tiên của SP). */
  private String thumbnailUrl;
  /** Ảnh cover / PDP chính — URL đầy đủ (Cloudinary hoặc tương đương). */
  private String mainImageUrl;
  /** Tất cả URL ảnh gallery ({@code document.type} = ảnh hoặc legacy 0), không trùng; thumbnail ưu tiên {@code isMain}. */
  private List<String> imageUrls;
  /**
   * Tóm tắt media (ảnh/video) của sản phẩm, thứ tự theo id document tăng dần. Xem {@link ProductDocumentSummary}.
   */
  private List<ProductDocumentSummary> documents;

  /** Alias storefront: cùng giá trị {@link #mainImageUrl}. Chỉ xuất JSON, không lưu field. */
  @JsonProperty(value = "imageUrl", access = JsonProperty.Access.READ_ONLY)
  public String getImageUrl() {
    return mainImageUrl;
  }

  /** Alias storefront (cover): cùng {@link #mainImageUrl}. */
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

  /**
   * @param variantDisplayUnitPrices map variantId → đơn giá hiển thị (price change + catalog); null = không gắn effective.
   */
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
