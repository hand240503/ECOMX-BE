package com.ndh.ShopTechnology.services.brand.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.annotation.AdminAudit;
import com.ndh.ShopTechnology.aspect.SnapshotFetcherRegistry;
import com.ndh.ShopTechnology.constants.DocumentEntityType;
import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.dto.request.brand.CreateBrandRequest;
import com.ndh.ShopTechnology.dto.request.brand.UpdateBrandRequest;
import com.ndh.ShopTechnology.dto.response.brand.BrandResponse;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.DocumentRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.brand.BrandService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final DocumentRepository documentRepository;
    private final SnapshotFetcherRegistry snapshotFetcherRegistry;
    private final ObjectMapper objectMapper;

    public BrandServiceImpl(BrandRepository brandRepository,
                            ProductRepository productRepository,
                            DocumentRepository documentRepository,
                            SnapshotFetcherRegistry snapshotFetcherRegistry,
                            ObjectMapper objectMapper) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.documentRepository = documentRepository;
        this.snapshotFetcherRegistry = snapshotFetcherRegistry;
        this.objectMapper = objectMapper;
    }

    /** Đăng ký snapshot fetcher để AOP lấy trạng thái trước khi UPDATE / DELETE */
    @PostConstruct
    public void registerSnapshotFetcher() {
        snapshotFetcherRegistry.register(AdminActivityLogEntity.ENTITY_BRAND, id ->
                brandRepository.findById(id).map(e -> {
                    try {
                        return objectMapper.writeValueAsString(BrandResponse.fromEntity(e, resolveLogo(e.getId())));
                    } catch (Exception ex) {
                        return null;
                    }
                }).orElse(null));
    }

    private String resolveLogo(Long brandId) {
        return documentRepository
                .findMainByEntityIdAndEntityType(brandId, DocumentEntityType.ID_DOCUMENT_ENTITY_BRAND)
                .map(d -> d.getFilePath())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> listAll() {
        return brandRepository.findAllByOrderByIdAsc().stream()
                .map(e -> BrandResponse.fromEntity(e, resolveLogo(e.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getById(long id) {
        BrandEntity e = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Brand not found with id: " + id));
        return BrandResponse.fromEntity(e, resolveLogo(id));
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_BRAND,
        action     = AdminActivityLogEntity.ACTION_CREATE,
        idArgIndex = -1
    )
    public BrandResponse create(CreateBrandRequest request) {
        String code = normalizeCode(request.getCode());
        String name = normalizeName(request.getName());
        if (brandRepository.existsByCodeIgnoreCase(code)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Brand code already exists: " + code);
        }
        int status = request.getStatus() != null ? request.getStatus() : SystemConstant.ACTIVE_STATUS;
        BrandEntity e = BrandEntity.builder()
                .code(code)
                .name(name)
                .status(status)
                .build();
        BrandEntity saved = brandRepository.save(e);
        return BrandResponse.fromEntity(saved, resolveLogo(saved.getId()));
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType            = AdminActivityLogEntity.ENTITY_BRAND,
        action                = AdminActivityLogEntity.ACTION_UPDATE,
        idArgIndex            = 0,
        captureSnapshotBefore = true
    )
    public BrandResponse update(long id, UpdateBrandRequest request) {
        BrandEntity e = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Brand not found with id: " + id));
        if (request.getCode() != null) {
            String code = normalizeCode(request.getCode());
            if (brandRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new CustomApiException(HttpStatus.CONFLICT, "Brand code already exists: " + code);
            }
            e.setCode(code);
        }
        if (request.getName() != null) {
            e.setName(normalizeName(request.getName()));
        }
        if (request.getStatus() != null) {
            e.setStatus(request.getStatus());
        }
        return BrandResponse.fromEntity(brandRepository.save(e), resolveLogo(id));
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType            = AdminActivityLogEntity.ENTITY_BRAND,
        action                = AdminActivityLogEntity.ACTION_DELETE,
        idArgIndex            = 0,
        captureSnapshotBefore = true
    )
    public void delete(long id) {
        brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Brand not found with id: " + id));
        // Gỡ thương hiệu khỏi sản phẩm (set null) trước khi xóa.
        productRepository.clearBrandForBrandIds(List.of(id));
        // EntityManager đã được clear (clearAutomatically) -> nạp lại để xóa an toàn.
        brandRepository.findById(id).ifPresent(brandRepository::delete);
    }

    @Override
    @Transactional
    @AdminAudit(
        entityType = AdminActivityLogEntity.ENTITY_BRAND,
        action     = AdminActivityLogEntity.ACTION_DELETE,
        idArgIndex = -1
    )
    public com.ndh.ShopTechnology.dto.response.brand.BrandBulkDeleteResponse deleteBrands(List<Long> ids) {
        List<Long> requested = (ids == null) ? List.of() : ids.stream()
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (requested.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Danh sách thương hiệu cần xóa đang trống");
        }

        List<BrandEntity> found = brandRepository.findAllById(requested);
        List<Long> foundIds = found.stream().map(BrandEntity::getId).collect(Collectors.toList());
        List<Long> notFound = requested.stream()
                .filter(rid -> !foundIds.contains(rid)).collect(Collectors.toList());

        int productsDetached = 0, deleted = 0;
        if (!foundIds.isEmpty()) {
            // Gỡ sản phẩm (brand=null) cho toàn bộ thương hiệu cần xóa.
            productsDetached = productRepository.clearBrandForBrandIds(foundIds);
            // EntityManager đã clear -> nạp lại bản sạch rồi xóa.
            List<BrandEntity> fresh = brandRepository.findAllById(foundIds);
            brandRepository.deleteAll(fresh);
            deleted = fresh.size();
        }

        return com.ndh.ShopTechnology.dto.response.brand.BrandBulkDeleteResponse.builder()
                .requested(requested.size())
                .deleted(deleted)
                .productsDetached(productsDetached)
                .notFoundIds(notFound)
                .build();
    }

    private static String normalizeCode(String raw) {
        if (raw == null) return "";
        String t = raw.trim().toUpperCase();
        if (t.isEmpty()) throw new CustomApiException(HttpStatus.BAD_REQUEST, "code must not be blank");
        return t;
    }

    private static String normalizeName(String raw) {
        if (raw == null) return "";
        String t = raw.trim();
        if (t.isEmpty()) throw new CustomApiException(HttpStatus.BAD_REQUEST, "name must not be blank");
        return t;
    }
}
