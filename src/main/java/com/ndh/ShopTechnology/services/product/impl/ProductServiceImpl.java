package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.search.ProductSearchResult;
import com.ndh.ShopTechnology.dto.response.product.ProductDetailResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.spec.ProductSearchSpecifications;
import com.ndh.ShopTechnology.repository.UnitRepository;
import com.ndh.ShopTechnology.repository.UserRatingRepository;
import com.ndh.ShopTechnology.repository.projection.ProductRatingAggregate;
import com.ndh.ShopTechnology.services.product.ProductService;
import com.ndh.ShopTechnology.services.recommendation.HybridRecommendationService;
import com.ndh.ShopTechnology.services.recommendation.RecommendationEnrichmentService;
import com.ndh.ShopTechnology.dto.request.product.CreatePriceRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final int MAX_SEARCH_QUERY_LENGTH = 200;
    private static final int MAX_SEARCH_TOKENS = 12;
    private static final int MAX_TOKEN_LENGTH = 64;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final UserRatingRepository userRatingRepository;
    private final HybridRecommendationService hybridRecommendationService;
    private final RecommendationEnrichmentService recommendationEnrichmentService;

    public ProductServiceImpl(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UnitRepository unitRepository,
            UserRatingRepository userRatingRepository,
            HybridRecommendationService hybridRecommendationService,
            RecommendationEnrichmentService recommendationEnrichmentService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.userRatingRepository = userRatingRepository;
        this.hybridRecommendationService = hybridRecommendationService;
        this.recommendationEnrichmentService = recommendationEnrichmentService;
    }

    @Override
    @Transactional
    public ProductFullResponse createProduct(CreateProductRequest request) {
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new NotFoundEntityException("Category not found with id: " + request.getCategoryId()));

        ProductEntity product = ProductEntity.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .status(request.getStatus())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .category(category)
                .build();

        if (request.getPrices() != null && !request.getPrices().isEmpty()) {
            for (CreatePriceRequest priceRequest : request.getPrices()) {
                UnitEntity unit = unitRepository.findById(priceRequest.getUnitId())
                        .orElseThrow(() -> new NotFoundEntityException(
                                "Unit not found with id: " + priceRequest.getUnitId()));

                PriceEntity price = PriceEntity.builder()
                        .unit(unit)
                        .product(product)
                        .currentValue(priceRequest.getCurrentValue())
                        .oldValue(priceRequest.getOldValue() != null ? priceRequest.getOldValue() : 0.0)
                        .build();

                product.getPrices().add(price);
            }
        }

        product = productRepository.save(product);
        ProductEntity full = productRepository.findWithFullRelationsById(product.getId())
                .orElse(product);
        return toFullWithRatings(full);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getAllProducts() {
        List<ProductEntity> products = productRepository.findAllWithListRelations();
        return mapWithRatings(products);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductFullResponse> getProducts(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        Page<ProductEntity> productPage = productRepository.findPageWithListRelations(pageable);
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                productPage.getContent().stream().map(ProductEntity::getId).toList());
        return productPage.map(p -> toFull(p, ratings));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getFeaturedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        List<ProductEntity> products = productRepository.findByIsFeaturedTrue(pageable);
        return mapWithRatings(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getBestSellingProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ProductEntity> products = productRepository.findTopNByOrderBySoldCountDesc(pageable);
        return mapWithRatings(products);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductFullResponse getProductById(Long id) {
        ProductEntity product = productRepository.findWithFullRelationsById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));
        return toFullWithRatings(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : productIds) {
            if (id == null) {
                continue;
            }
            seen.add(id);
            if (seen.size() >= 200) {
                break;
            }
        }
        if (seen.isEmpty()) {
            return List.of();
        }
        List<Long> orderedUnique = new ArrayList<>(seen);
        List<ProductEntity> loaded = productRepository.findAllWithFullRelationsByIdIn(orderedUnique);
        loaded.forEach(p -> Hibernate.initialize(p.getPolicies()));
        Map<Long, ProductEntity> byId = loaded.stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(orderedUnique);
        List<ProductFullResponse> out = new ArrayList<>();
        for (Long id : orderedUnique) {
            ProductEntity p = byId.get(id);
            if (p != null) {
                out.add(toFull(p, ratings));
            }
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long id, Long userId, String sessionId,
            int recommendationLimit) {
        ProductFullResponse product = getProductById(id);
        int recCap = recommendationLimit <= 0 ? 10 : Math.min(recommendationLimit, 50);
        List<ProductFullResponse> recommendations = List.of();
        Integer hybridId = toHybridProductId(id);
        if (hybridId != null) {
            int fetchCap = Math.min(recCap + 15, 100);
            List<ProductFullResponse> enriched = recommendationEnrichmentService.enrich(
                    hybridRecommendationService.getSimilarToProduct(hybridId, userId, sessionId, fetchCap));
            recommendations = enriched.stream()
                    .filter(p -> p.getId() != null && !p.getId().equals(id))
                    .limit(recCap)
                    .collect(Collectors.toList());
        }
        return ProductDetailResponse.builder()
                .product(product)
                .recommendations(recommendations)
                .build();
    }

    /**
     * Hybrid PDP recommendations use {@code int} product ids internally; wider ids return no recs.
     */
    private static Integer toHybridProductId(Long id) {
        if (id == null || id < 1L || id > (long) Integer.MAX_VALUE) {
            return null;
        }
        return id.intValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductFullResponse> getProductsByCategoryId(Long categoryId, int page, int limit) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundEntityException("Category not found with id: " + categoryId);
        }
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        boolean isParentCategory = categoryRepository.existsByParent_Id(categoryId);
        Page<ProductEntity> productPage = isParentCategory
                ? productRepository.findPageByDirectChildCategoriesOf(categoryId, pageable)
                : productRepository.findPageByCategoryId(categoryId, pageable);
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                productPage.getContent().stream().map(ProductEntity::getId).toList());
        return productPage.map(p -> toFull(p, ratings));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSearchResult searchProducts(String query, int page, int limit) {
        String q = sanitizeSearchKeyword(query);
        Sort relevance = Sort.by(Sort.Direction.DESC, "soldCount")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(limit, 1), relevance);
        if (q.isEmpty()) {
            return new ProductSearchResult(Page.empty(pageable), null);
        }
        List<String> tokens = tokenizeSearchKeywords(q);
        if (tokens.isEmpty()) {
            return new ProductSearchResult(Page.empty(pageable), null);
        }
        Specification<ProductEntity> spec = Specification
                .where(ProductSearchSpecifications.isActiveOrNullStatus(SystemConstant.ACTIVE_STATUS))
                .and(ProductSearchSpecifications.allTokensMatchNameDescriptionTag(tokens));
        Page<ProductEntity> productPage = productRepository.findAll(spec, pageable);
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                productPage.getContent().stream().map(ProductEntity::getId).toList());
        Page<ProductFullResponse> mapped = productPage.map(p -> toFull(p, ratings));
        return new ProductSearchResult(mapped, null);
    }

    /**
     * Splits on whitespace; each token is matched (AND) against name / description / tag so spaced
     * queries still hit compact product titles (e.g. "Smart Phone" vs "Smartphone").
     */
    private static List<String> tokenizeSearchKeywords(String sanitizedQuery) {
        String[] parts = sanitizedQuery.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String t = part.replace("%", "").replace("_", "").trim().toLowerCase();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > MAX_TOKEN_LENGTH) {
                t = t.substring(0, MAX_TOKEN_LENGTH);
            }
            tokens.add(t);
            if (tokens.size() >= MAX_SEARCH_TOKENS) {
                break;
            }
        }
        return tokens;
    }

    /**
     * Trims and strips LIKE wildcards so user input cannot broaden the pattern; caps length.
     */
    private static String sanitizeSearchKeyword(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        t = t.replace("%", " ").replace("_", " ").replaceAll("\\s+", " ").trim();
        if (t.length() > MAX_SEARCH_QUERY_LENGTH) {
            t = t.substring(0, MAX_SEARCH_QUERY_LENGTH).trim();
        }
        return t;
    }

    @Override
    @Transactional
    public ProductFullResponse updateProduct(Long id, UpdateProductRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));

        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        product = productRepository.save(product);
        ProductEntity full = productRepository.findWithFullRelationsById(product.getId())
                .orElse(product);
        return toFullWithRatings(full);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    private List<ProductFullResponse> mapWithRatings(List<ProductEntity> products) {
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                products.stream().map(ProductEntity::getId).toList());
        return products.stream().map(p -> toFull(p, ratings)).collect(Collectors.toList());
    }

    private Map<Long, ProductRatingAggregate> ratingMapForIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userRatingRepository.aggregateByProductIdIn(ids).stream()
                .collect(Collectors.toMap(ProductRatingAggregate::getProductId, Function.identity()));
    }

    private ProductFullResponse toFullWithRatings(ProductEntity p) {
        Hibernate.initialize(p.getPolicies());
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(List.of(p.getId()));
        return toFull(p, ratings);
    }

    private static ProductFullResponse toFull(ProductEntity p, Map<Long, ProductRatingAggregate> ratings) {
        ProductRatingAggregate a = ratings.get(p.getId());
        Double avg = a != null ? a.getAverageRating() : null;
        Long cnt = a != null ? a.getRatingCount() : null;
        return ProductFullResponse.fromEntity(p, null, null, avg, cnt);
    }
}
