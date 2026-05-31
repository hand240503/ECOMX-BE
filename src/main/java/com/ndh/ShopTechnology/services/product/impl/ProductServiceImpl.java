package com.ndh.ShopTechnology.services.product.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.annotation.AdminAudit;
import com.ndh.ShopTechnology.aspect.SnapshotFetcherRegistry;
import com.ndh.ShopTechnology.constants.DocumentEntityType;
import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.search.ProductSearchResult;
import com.ndh.ShopTechnology.dto.response.product.ProductDetailResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.spec.ProductSearchSpecifications;
import com.ndh.ShopTechnology.repository.UnitRepository;
import com.ndh.ShopTechnology.repository.UserRatingRepository;
import com.ndh.ShopTechnology.repository.projection.ProductRatingAggregate;
import com.ndh.ShopTechnology.services.doc.DocumentService;
import com.ndh.ShopTechnology.services.product.ProductPriceService;
import com.ndh.ShopTechnology.services.product.ProductService;
import com.ndh.ShopTechnology.services.recommendation.HybridRecommendationService;
import com.ndh.ShopTechnology.services.recommendation.RecommendationEnrichmentService;
import com.ndh.ShopTechnology.dto.request.product.CreatePriceRequest;
import com.ndh.ShopTechnology.dto.request.product.CreateProductVariantRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductVariantItemRequest;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductPriceItemRequest;
import com.ndh.ShopTechnology.dto.request.product.UpsertProductPriceRequest;
import com.ndh.ShopTechnology.repository.ProductPriceChangeRepository;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.repository.ProductVolumePriceTierRepository;
import com.ndh.ShopTechnology.repository.PurchaseWithPurchaseOfferRepository;
import com.ndh.ShopTechnology.dto.response.product.ActivePromotionsResponse;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;

import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.hibernate.Hibernate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final int MAX_SEARCH_QUERY_LENGTH = 200;
    private static final int MAX_SEARCH_TOKENS = 12;
    private static final int MAX_TOKEN_LENGTH = 64;

    public static final int MAX_FLAGGED_PRODUCT_LIST_LIMIT = 500;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UnitRepository unitRepository;
    private final UserRatingRepository userRatingRepository;
    private final HybridRecommendationService hybridRecommendationService;
    private final RecommendationEnrichmentService recommendationEnrichmentService;
    private final ProductImageAttachService productImageAttachService;
    private final DocumentService documentService;
    private final ProductPriceService productPriceService;
    private final ProductVariantPriceHydrator productVariantPriceHydrator;
    private final VariantDisplayPriceResolver variantDisplayPriceResolver;
    private final ProductPricingProgramsAttachService productPricingProgramsAttachService;
    private final ProductVariantRepository productVariantRepository;
    private final ProductPriceChangeRepository productPriceChangeRepository;
    private final ProductVolumePriceTierRepository productVolumePriceTierRepository;
    private final PurchaseWithPurchaseOfferRepository purchaseWithPurchaseOfferRepository;
    private final SnapshotFetcherRegistry snapshotFetcherRegistry;
    private final ObjectMapper objectMapper;

    private static final int LEGACY_PRODUCT_DOCUMENT_ENTITY_TYPE = 1;

    @PostConstruct
    public void registerSnapshotFetcher() {
        snapshotFetcherRegistry.register(AdminActivityLogEntity.ENTITY_PRODUCT, id ->
                productRepository.findById(id).map(e -> {
                    try {
                        return objectMapper.writeValueAsString(
                                java.util.Map.of("id", e.getId(), "productName", e.getProductName(),
                                        "status", e.getStatus()));
                    } catch (Exception ex) {
                        return null;
                    }
                }).orElse(null));
    }

    public ProductServiceImpl(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            UnitRepository unitRepository,
            UserRatingRepository userRatingRepository,
            HybridRecommendationService hybridRecommendationService,
            RecommendationEnrichmentService recommendationEnrichmentService,
            ProductImageAttachService productImageAttachService,
            DocumentService documentService,
            ProductPriceService productPriceService,
            ProductVariantPriceHydrator productVariantPriceHydrator,
            VariantDisplayPriceResolver variantDisplayPriceResolver,
            ProductPricingProgramsAttachService productPricingProgramsAttachService,
            ProductVariantRepository productVariantRepository,
            ProductPriceChangeRepository productPriceChangeRepository,
            ProductVolumePriceTierRepository productVolumePriceTierRepository,
            PurchaseWithPurchaseOfferRepository purchaseWithPurchaseOfferRepository,
            SnapshotFetcherRegistry snapshotFetcherRegistry,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.unitRepository = unitRepository;
        this.userRatingRepository = userRatingRepository;
        this.hybridRecommendationService = hybridRecommendationService;
        this.recommendationEnrichmentService = recommendationEnrichmentService;
        this.productImageAttachService = productImageAttachService;
        this.documentService = documentService;
        this.productPriceService = productPriceService;
        this.productVariantPriceHydrator = productVariantPriceHydrator;
        this.variantDisplayPriceResolver = variantDisplayPriceResolver;
        this.productPricingProgramsAttachService = productPricingProgramsAttachService;
        this.productVariantRepository = productVariantRepository;
        this.productPriceChangeRepository = productPriceChangeRepository;
        this.productVolumePriceTierRepository = productVolumePriceTierRepository;
        this.purchaseWithPurchaseOfferRepository = purchaseWithPurchaseOfferRepository;
        this.snapshotFetcherRegistry = snapshotFetcherRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_PRODUCT,
        action     = AdminActivityLogEntity.ACTION_CREATE,
        idArgIndex = -1
    )
    public ProductFullResponse createProduct(CreateProductRequest request) {
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new NotFoundEntityException("Category not found with id: " + request.getCategoryId()));

        BrandEntity brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Brand not found with id: " + request.getBrandId()));
        }

        ProductEntity product = ProductEntity.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .lDescription(request.getLDescription())
                .status(request.getStatus())
                .sku(request.getSku())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .hotSale(request.getHotSale() != null ? request.getHotSale() : false)
                .category(category)
                .brand(brand)
                .build();

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (CreateProductVariantRequest vr : request.getVariants()) {
                if (vr == null) {
                    continue;
                }
                product.getVariants().add(buildVariant(product, vr));
            }
        } else if (request.getPrices() != null && !request.getPrices().isEmpty()) {
            ProductVariantEntity defaultVariant = ProductVariantEntity.builder()
                    .product(product)
                    .skuCode(request.getSku() != null ? String.valueOf(request.getSku()) : null)
                    .active(true)
                    .sortOrder(0)
                    .build();
            for (CreatePriceRequest priceRequest : request.getPrices()) {
                UnitEntity unit = unitRepository.findById(priceRequest.getUnitId())
                        .orElseThrow(() -> new NotFoundEntityException(
                                "Unit not found with id: " + priceRequest.getUnitId()));
                PriceEntity price = PriceEntity.builder()
                        .unit(unit)
                        .variant(defaultVariant)
                        .currentValue(priceRequest.getCurrentValue())
                        .oldValue(priceRequest.getOldValue() != null ? priceRequest.getOldValue() : 0.0)
                        .build();
                defaultVariant.getPrices().add(price);
            }
            product.getVariants().add(defaultVariant);
        } else {
            product.getVariants().add(ProductVariantEntity.builder()
                    .product(product)
                    .active(true)
                    .sortOrder(0)
                    .build());
        }

        product = productRepository.save(product);
        ProductEntity full = productRepository.findWithFullRelationsById(product.getId())
                .orElse(product);
        productVariantPriceHydrator.attachPrices(List.of(full));
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
        List<ProductEntity> hydrated = hydrateProductsWithVariants(productPage.getContent());
        Page<ProductEntity> hydratedPage = new PageImpl<>(hydrated, pageable, productPage.getTotalElements());
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                hydratedPage.getContent().stream().map(ProductEntity::getId).toList());
        Map<Long, Double> displayUnitPrices =
                variantDisplayPriceResolver.resolveForProducts(hydratedPage.getContent());
        Page<ProductFullResponse> mapped = hydratedPage.map(p -> toFull(p, ratings, displayUnitPrices));
        attachProductPresentation(mapped.getContent());
        return mapped;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductFullResponse> getAdminProducts(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, "id"));
        Page<ProductEntity> productPage = productRepository.findPageWithListRelations(pageable);
        List<ProductEntity> hydrated = hydrateProductsWithVariants(productPage.getContent());
        Page<ProductEntity> hydratedPage = new PageImpl<>(hydrated, pageable, productPage.getTotalElements());
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                hydratedPage.getContent().stream().map(ProductEntity::getId).toList());
        Map<Long, Double> displayUnitPrices =
                variantDisplayPriceResolver.resolveForProducts(hydratedPage.getContent());
        Page<ProductFullResponse> mapped = hydratedPage.map(p -> toFull(p, ratings, displayUnitPrices));
        attachProductPresentation(mapped.getContent());
        return mapped;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getFeaturedProducts(int limit, boolean all) {
        List<ProductEntity> products = all
                ? productRepository.findByIsFeaturedTrueOrderByIdDesc()
                : productRepository.findByIsFeaturedTrue(PageRequest.of(
                        0,
                        effectiveFlaggedListLimit(limit),
                        Sort.by(Sort.Direction.DESC, "id")));
        return mapWithRatings(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFullResponse> getHotSaleProducts(int limit, boolean all) {
        List<ProductEntity> products = all
                ? productRepository.findByHotSaleTrueOrderByIdDesc()
                : productRepository.findByHotSaleTrue(PageRequest.of(
                        0,
                        effectiveFlaggedListLimit(limit),
                        Sort.by(Sort.Direction.DESC, "id")));
        return mapWithRatings(products);
    }

    private static int effectiveFlaggedListLimit(int limit) {
        int l = limit <= 0 ? 10 : limit;
        return Math.min(l, MAX_FLAGGED_PRODUCT_LIST_LIMIT);
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
        productVariantPriceHydrator.attachPrices(List.of(product));
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
        productVariantPriceHydrator.attachPrices(loaded);
        loaded.forEach(p -> Hibernate.initialize(p.getPolicies()));
        Map<Long, ProductEntity> byId = loaded.stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(orderedUnique);
        Map<Long, Double> displayUnitPrices = variantDisplayPriceResolver.resolveForProducts(loaded);
        List<ProductFullResponse> out = new ArrayList<>();
        for (Long id : orderedUnique) {
            ProductEntity p = byId.get(id);
            if (p != null) {
                out.add(toFull(p, ratings, displayUnitPrices));
            }
        }
        attachProductPresentation(out);
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
        List<ProductEntity> hydrated = hydrateProductsWithVariants(productPage.getContent());
        Page<ProductEntity> hydratedPage = new PageImpl<>(hydrated, pageable, productPage.getTotalElements());
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                hydratedPage.getContent().stream().map(ProductEntity::getId).toList());
        Map<Long, Double> displayUnitPrices =
                variantDisplayPriceResolver.resolveForProducts(hydratedPage.getContent());
        Page<ProductFullResponse> mapped = hydratedPage.map(p -> toFull(p, ratings, displayUnitPrices));
        attachProductPresentation(mapped.getContent());
        return mapped;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductFullResponse> getAdminProductsByCategoryId(Long categoryId, int page, int limit) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundEntityException("Category not found with id: " + categoryId);
        }
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, "id"));
        boolean isParentCategory = categoryRepository.existsByParent_Id(categoryId);
        Page<ProductEntity> productPage = isParentCategory
                ? productRepository.findPageByDirectChildCategoriesOf(categoryId, pageable)
                : productRepository.findPageByCategoryId(categoryId, pageable);
        List<ProductEntity> hydrated = hydrateProductsWithVariants(productPage.getContent());
        Page<ProductEntity> hydratedPage = new PageImpl<>(hydrated, pageable, productPage.getTotalElements());
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                hydratedPage.getContent().stream().map(ProductEntity::getId).toList());
        Map<Long, Double> displayUnitPrices =
                variantDisplayPriceResolver.resolveForProducts(hydratedPage.getContent());
        Page<ProductFullResponse> mapped = hydratedPage.map(p -> toFull(p, ratings, displayUnitPrices));
        attachProductPresentation(mapped.getContent());
        return mapped;
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
        List<ProductEntity> hydrated = hydrateProductsWithVariants(productPage.getContent());
        Page<ProductEntity> hydratedPage = new PageImpl<>(hydrated, pageable, productPage.getTotalElements());
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                hydratedPage.getContent().stream().map(ProductEntity::getId).toList());
        Map<Long, Double> displayUnitPrices =
                variantDisplayPriceResolver.resolveForProducts(hydratedPage.getContent());
        Page<ProductFullResponse> mapped = hydratedPage.map(p -> toFull(p, ratings, displayUnitPrices));
        attachProductPresentation(mapped.getContent());
        return new ProductSearchResult(mapped, null);
    }

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
    @AdminAudit(
        entityType            = AdminActivityLogEntity.ENTITY_PRODUCT,
        action                = AdminActivityLogEntity.ACTION_UPDATE,
        idArgIndex            = 0,
        captureSnapshotBefore = true
    )
    public ProductFullResponse updateProduct(Long id, UpdateProductRequest request, List<MultipartFile> newImages) {
        ProductEntity product = loadProductGraphForUpdate(id);
        applyScalarUpdates(product, request);
        applyVariantMutations(product, request);
        productRepository.saveAndFlush(product);

        applyLegacyRootPriceMutations(product, request);
        deleteRemovedProductDocuments(id, request);
        persistNewProductImages(id, request, newImages);
        promoteMainDocumentIfRequested(id, request);

        ProductEntity full = loadProductGraphForUpdate(id);
        return toFullWithRatings(full);
    }

    private ProductEntity loadProductGraphForUpdate(long productId) {
        ProductEntity product = productRepository.findWithFullRelationsById(productId)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + productId));
        productVariantPriceHydrator.attachPrices(List.of(product));
        return product;
    }

    private void deleteRemovedProductDocuments(long productId, UpdateProductRequest request) {
        List<Long> toRemove = request.getRemovedDocumentIds() == null
                ? List.of()
                : request.getRemovedDocumentIds().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        for (Long docId : toRemove) {
            assertDocumentBelongsToProduct(productId, docId);
            documentService.deleteDocument(docId);
        }
    }

    private void persistNewProductImages(long productId, UpdateProductRequest request, List<MultipartFile> newImages) {
        List<MultipartFile> filesToAdd = newImages == null
                ? List.of()
                : newImages.stream().filter(f -> f != null && !f.isEmpty()).toList();

        Integer mainIdx = request.getMainNewImageIndex();
        if (mainIdx != null) {
            if (filesToAdd.isEmpty()) {
                throw new CustomApiException(
                        HttpStatus.BAD_REQUEST, "mainNewImageIndex requires at least one new image (newImages)");
            }
            if (mainIdx < 0 || mainIdx >= filesToAdd.size()) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "mainNewImageIndex out of range for newImages");
            }
        }

        if (filesToAdd.isEmpty()) {
            return;
        }
        try {
            documentService.persistUploadedFiles(
                    filesToAdd,
                    productId,
                    DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT,
                    mainIdx);
        } catch (IOException e) {
            throw new CustomApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload product images: " + e.getMessage());
        }
    }

    private void promoteMainDocumentIfRequested(long productId, UpdateProductRequest request) {
        if (request.getMainDocumentId() == null) {
            return;
        }
        assertDocumentBelongsToProduct(productId, request.getMainDocumentId());
        documentService.setMainDocument(request.getMainDocumentId());
    }

    private void applyScalarUpdates(ProductEntity product, UpdateProductRequest request) {
        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getLDescription() != null) {
            product.setLDescription(request.getLDescription());
        }

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }

        if (request.getHotSale() != null) {
            product.setHotSale(request.getHotSale());
        }

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            BrandEntity brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            "Brand not found with id: " + request.getBrandId()));
            product.setBrand(brand);
        }

        if (request.getTag() != null) {
            product.setTag(request.getTag());
        }

        if (request.getSku() != null) {
            product.setSku(request.getSku());
        }
    }

    private void applyLegacyRootPriceMutations(ProductEntity product, UpdateProductRequest request) {
        Long productId = product.getId();
        boolean touchesLegacyPrices = (request.getRemovedPriceIds() != null && !request.getRemovedPriceIds().isEmpty())
                || (request.getUpdatedPrices() != null && !request.getUpdatedPrices().isEmpty())
                || (request.getNewPrices() != null && !request.getNewPrices().isEmpty());
        if (!touchesLegacyPrices) {
            return;
        }
        ProductVariantEntity target = primaryVariant(product);
        if (request.getRemovedPriceIds() != null) {
            for (Long priceId :
                    request.getRemovedPriceIds().stream().filter(Objects::nonNull).distinct().toList()) {
                productPriceService.delete(productId, priceId.longValue());
            }
        }
        if (request.getUpdatedPrices() != null) {
            for (UpdateProductPriceItemRequest item : request.getUpdatedPrices()) {
                if (item == null || item.getId() == null) {
                    continue;
                }
                UpsertProductPriceRequest body = UpsertProductPriceRequest.builder()
                        .productVariantId(target.getId())
                        .unitId(item.getUnitId())
                        .currentValue(item.getCurrentValue())
                        .oldValue(item.getOldValue())
                        .build();
                productPriceService.update(productId, item.getId(), body);
            }
        }
        if (request.getNewPrices() != null) {
            for (CreatePriceRequest cr : request.getNewPrices()) {
                if (cr == null) {
                    continue;
                }
                UpsertProductPriceRequest body = UpsertProductPriceRequest.builder()
                        .productVariantId(target.getId())
                        .unitId(cr.getUnitId())
                        .currentValue(cr.getCurrentValue())
                        .oldValue(cr.getOldValue())
                        .build();
                productPriceService.create(productId, body);
            }
        }
    }

    private void applyVariantMutations(ProductEntity product, UpdateProductRequest request) {
        if (request.getRemovedVariantIds() != null) {
            for (Long vid :
                    request.getRemovedVariantIds().stream().filter(Objects::nonNull).distinct().toList()) {
                if (product.getVariants().size() <= 1) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST, "Cannot remove the last variant");
                }
                boolean removed = product.getVariants().removeIf(v -> vid.equals(v.getId()));
                if (!removed) {
                    throw new NotFoundEntityException("Variant not found on product: " + vid);
                }
            }
        }
        if (request.getNewVariants() != null) {
            for (CreateProductVariantRequest nv : request.getNewVariants()) {
                if (nv == null) {
                    continue;
                }
                product.getVariants().add(buildVariant(product, nv));
            }
        }
        if (request.getUpdatedVariants() != null) {
            Long pid = product.getId();
            for (UpdateProductVariantItemRequest uv : request.getUpdatedVariants()) {
                if (uv == null || uv.getId() == null) {
                    continue;
                }
                ProductVariantEntity v = product.getVariants().stream()
                        .filter(x -> uv.getId().equals(x.getId()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundEntityException("Variant not found: " + uv.getId()));
                if (uv.getSkuCode() != null) {
                    v.setSkuCode(uv.getSkuCode());
                }
                if (uv.getOptionValues() != null) {
                    v.setOptionValues(new java.util.LinkedHashMap<>(uv.getOptionValues()));
                }
                if (uv.getActive() != null) {
                    v.setActive(uv.getActive());
                }
                if (uv.getSortOrder() != null) {
                    v.setSortOrder(uv.getSortOrder());
                }
                if (uv.getRemovedPriceIds() != null) {
                    for (Long priceId :
                            uv.getRemovedPriceIds().stream().filter(Objects::nonNull).distinct().toList()) {
                        productPriceService.delete(pid, priceId.longValue());
                    }
                }
                if (uv.getUpdatedPrices() != null) {
                    for (UpdateProductPriceItemRequest item : uv.getUpdatedPrices()) {
                        if (item == null || item.getId() == null) {
                            continue;
                        }
                        UpsertProductPriceRequest body = UpsertProductPriceRequest.builder()
                                .unitId(item.getUnitId())
                                .currentValue(item.getCurrentValue())
                                .oldValue(item.getOldValue())
                                .build();
                        productPriceService.update(pid, item.getId(), body);
                    }
                }
                if (uv.getNewPrices() != null) {
                    for (CreatePriceRequest cr : uv.getNewPrices()) {
                        if (cr == null) {
                            continue;
                        }
                        UpsertProductPriceRequest body = UpsertProductPriceRequest.builder()
                                .productVariantId(v.getId())
                                .unitId(cr.getUnitId())
                                .currentValue(cr.getCurrentValue())
                                .oldValue(cr.getOldValue())
                                .build();
                        productPriceService.create(pid, body);
                    }
                }
                if (uv.getRemovedDocumentIds() != null) {
                    for (Long docId :
                            uv.getRemovedDocumentIds().stream().filter(Objects::nonNull).distinct().toList()) {
                        assertVariantMediaDocumentBelongsToProduct(pid, uv.getId(), docId);
                        documentService.deleteDocument(docId);
                    }
                }
                if (uv.getMainDocumentId() != null) {
                    assertVariantMediaDocumentBelongsToProduct(pid, uv.getId(), uv.getMainDocumentId());
                    documentService.setMainDocument(uv.getMainDocumentId());
                }
            }
        }
    }

    private ProductVariantEntity buildVariant(ProductEntity product, CreateProductVariantRequest req) {
        ProductVariantEntity v = ProductVariantEntity.builder()
                .product(product)
                .skuCode(req.getSkuCode())
                .optionValues(req.getOptionValues() != null
                        ? new java.util.LinkedHashMap<>(req.getOptionValues())
                        : new java.util.LinkedHashMap<>())
                .active(req.getActive() != null ? req.getActive() : true)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();
        if (req.getPrices() != null) {
            for (CreatePriceRequest priceRequest : req.getPrices()) {
                if (priceRequest == null) {
                    continue;
                }
                UnitEntity unit = unitRepository.findById(priceRequest.getUnitId())
                        .orElseThrow(() -> new NotFoundEntityException(
                                "Unit not found with id: " + priceRequest.getUnitId()));
                PriceEntity price = PriceEntity.builder()
                        .unit(unit)
                        .variant(v)
                        .currentValue(priceRequest.getCurrentValue())
                        .oldValue(priceRequest.getOldValue() != null ? priceRequest.getOldValue() : 0.0)
                        .build();
                v.getPrices().add(price);
            }
        }
        return v;
    }

    private static ProductVariantEntity primaryVariant(ProductEntity product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Product has no variants");
        }
        return product.getVariants().stream()
                .min(Comparator.comparing(ProductVariantEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductVariantEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .orElseThrow(() -> new CustomApiException(HttpStatus.BAD_REQUEST, "Product has no variants"));
    }

    private List<ProductEntity> hydrateProductsWithVariants(Collection<ProductEntity> slice) {
        if (slice == null || slice.isEmpty()) {
            return List.of();
        }
        List<Long> ids = slice.stream().map(ProductEntity::getId).filter(Objects::nonNull).toList();
        List<ProductEntity> full = productRepository.findAllWithFullRelationsByIdIn(ids);
        productVariantPriceHydrator.attachPrices(full);
        Map<Long, ProductEntity> m = full.stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        List<ProductEntity> out = new ArrayList<>();
        for (ProductEntity p : slice) {
            out.add(m.getOrDefault(p.getId(), p));
        }
        return out;
    }

    private void assertVariantMediaDocumentBelongsToProduct(Long productId, Long variantId, Long documentId) {
        if (!productVariantRepository.existsByIdAndProduct_Id(variantId, productId)) {
            throw new NotFoundEntityException("Variant not found on product: " + variantId);
        }
        DocumentEntity doc = documentService.getDocumentById(documentId);
        if (doc == null) {
            throw new NotFoundEntityException("Document not found with id: " + documentId);
        }
        if (!Objects.equals(doc.getEntityId(), variantId)
                || !Objects.equals(doc.getEntityType(), DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT_VARIANT)) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Document " + documentId + " is not variant media for variant " + variantId);
        }
    }

    private void assertDocumentBelongsToProduct(Long productId, Long documentId) {
        DocumentEntity doc = documentService.getDocumentById(documentId);
        if (doc == null) {
            throw new NotFoundEntityException("Document not found with id: " + documentId);
        }
        if (!Objects.equals(doc.getEntityId(), productId)) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Document " + documentId + " does not belong to product " + productId);
        }
        Integer et = doc.getEntityType();
        if (et == null
                || (et != DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT
                        && et != LEGACY_PRODUCT_DOCUMENT_ENTITY_TYPE)) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST, "Document " + documentId + " is not linked as product media");
        }
    }

    @Override
    @Transactional
    public ProductFullResponse addVariantImages(
            Long productId, Long variantId, List<MultipartFile> newImages, Integer mainNewImageIndex) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundEntityException("Product not found with id: " + productId);
        }
        if (!productVariantRepository.existsByIdAndProduct_Id(variantId, productId)) {
            throw new NotFoundEntityException("Variant not found on product: " + variantId);
        }
        List<MultipartFile> filesToAdd =
                newImages == null ? List.of() : newImages.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (filesToAdd.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "newImages must contain at least one file");
        }
        if (mainNewImageIndex != null) {
            if (mainNewImageIndex < 0 || mainNewImageIndex >= filesToAdd.size()) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "mainNewImageIndex out of range for newImages");
            }
        }
        try {
            documentService.persistUploadedFiles(
                    filesToAdd,
                    variantId,
                    DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT_VARIANT,
                    mainNewImageIndex);
        } catch (IOException e) {
            throw new CustomApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload variant images: " + e.getMessage());
        }
        ProductEntity product = productRepository.findWithFullRelationsById(productId)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + productId));
        productVariantPriceHydrator.attachPrices(List.of(product));
        return toFullWithRatings(product);
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType            = AdminActivityLogEntity.ENTITY_PRODUCT,
        action                = AdminActivityLogEntity.ACTION_DELETE,
        idArgIndex            = 0,
        captureSnapshotBefore = true
    )
    public void deleteProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    private List<ProductFullResponse> mapWithRatings(List<ProductEntity> products) {
        List<ProductEntity> hydrated = hydrateProductsWithVariants(products);
        Map<Long, ProductRatingAggregate> ratings = ratingMapForIds(
                hydrated.stream().map(ProductEntity::getId).toList());
        Map<Long, Double> displayUnitPrices = variantDisplayPriceResolver.resolveForProducts(hydrated);
        List<ProductFullResponse> list =
                hydrated.stream().map(p -> toFull(p, ratings, displayUnitPrices)).collect(Collectors.toList());
        attachProductPresentation(list);
        return list;
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
        Map<Long, Double> displayUnitPrices = variantDisplayPriceResolver.resolveForProducts(List.of(p));
        ProductFullResponse dto = toFull(p, ratings, displayUnitPrices);
        attachProductPresentation(List.of(dto));
        return dto;
    }

    private void attachProductPresentation(List<ProductFullResponse> products) {
        productImageAttachService.attach(products);
        productPricingProgramsAttachService.attach(products);
    }

    private static ProductFullResponse toFull(
            ProductEntity p,
            Map<Long, ProductRatingAggregate> ratings,
            Map<Long, Double> variantDisplayUnitPrices) {
        ProductRatingAggregate a = ratings.get(p.getId());
        Double avg = a != null ? a.getAverageRating() : null;
        Long cnt = a != null ? a.getRatingCount() : null;
        return ProductFullResponse.fromEntity(p, null, null, avg, cnt, variantDisplayUnitPrices);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivePromotionsResponse getActivePromotions() {
        java.util.Date now = new java.util.Date();

        List<Long> pcIds = productPriceChangeRepository.findDistinctProductIdsWithActivePCAt(now);

        List<Long> vtIds = productVolumePriceTierRepository.findDistinctProductIdsWithEnabledTiers();

        List<Long> pwpAnchorIds = purchaseWithPurchaseOfferRepository.findDistinctAnchorProductIdsEnabled();
        List<Long> pwpCompanionIds = purchaseWithPurchaseOfferRepository.findDistinctCompanionProductIdsEnabled();
        Set<Long> pwpSet = new LinkedHashSet<>();
        pwpSet.addAll(pwpAnchorIds);
        pwpSet.addAll(pwpCompanionIds);
        List<Long> pwpIds = new ArrayList<>(pwpSet);

        return ActivePromotionsResponse.builder()
                .priceChange(getProductsByIds(pcIds))
                .volumeTier(getProductsByIds(vtIds))
                .purchaseWithPurchase(getProductsByIds(pwpIds))
                .build();
    }
}
