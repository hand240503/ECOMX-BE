package com.ndh.ShopTechnology.services.catalog.impl;

import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Objects;

/**
 * Thực hiện upsert một thương hiệu / danh mục trong transaction RIÊNG (REQUIRES_NEW)
 * để mỗi dòng độc lập — dòng lỗi không ảnh hưởng dòng khác.
 */
@Component
public class CatalogUpsertPersister {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public CatalogUpsertPersister(BrandRepository brandRepository, CategoryRepository categoryRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    public static class Outcome {
        public final String action; // CREATED | UPDATED | SKIPPED
        public final Long id;
        Outcome(String action, Long id) { this.action = action; this.id = id; }
    }

    static class CatalogRowException extends RuntimeException {
        CatalogRowException(String m) { super(m); }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome upsertBrand(Long id, String code, String name, Integer status) {
        if (isBlank(name)) throw new CatalogRowException("Thiếu tên (name)");

        BrandEntity existing = null;
        if (id != null) existing = brandRepository.findById(id).orElse(null);
        if (existing == null && !isBlank(code)) {
            existing = brandRepository.findFirstByCodeIgnoreCase(code.trim()).orElse(null);
        }

        if (existing != null) {
            String newCode = isBlank(code) ? existing.getCode() : code.trim().toUpperCase();
            String newName = name.trim();
            Integer newStatus = status != null ? status : existing.getStatus();

            // SKIP: dữ liệu trong file giống hệt bản ghi hiện có -> không ghi đè.
            boolean unchanged = Objects.equals(trimToNull(existing.getCode()), trimToNull(newCode))
                    && Objects.equals(trimToNull(existing.getName()), trimToNull(newName))
                    && Objects.equals(existing.getStatus(), newStatus);
            if (unchanged) {
                return new Outcome("SKIPPED", existing.getId());
            }

            if (!isBlank(code)) {
                String c = code.trim().toUpperCase();
                if (brandRepository.existsByCodeIgnoreCaseAndIdNot(c, existing.getId())) {
                    throw new CatalogRowException("Trùng code với thương hiệu khác: " + c);
                }
                existing.setCode(c);
            }
            existing.setName(newName);
            if (status != null) existing.setStatus(status);
            BrandEntity saved = brandRepository.save(existing);
            return new Outcome("UPDATED", saved.getId());
        }

        String c = isBlank(code) ? uniqueBrandCode(name) : code.trim().toUpperCase();
        if (brandRepository.existsByCodeIgnoreCase(c)) {
            throw new CatalogRowException("Code đã tồn tại: " + c);
        }
        BrandEntity created = brandRepository.save(BrandEntity.builder()
                .code(c)
                .name(name.trim())
                .status(status != null ? status : SystemConstant.ACTIVE_STATUS)
                .build());
        return new Outcome("CREATED", created.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome upsertCategory(Long id, String code, String name, Integer status,
                                  Long parentId, String parentCode, String parentName) {
        if (isBlank(name)) throw new CatalogRowException("Thiếu tên (name)");

        CategoryEntity existing = null;
        if (id != null) existing = categoryRepository.findById(id).orElse(null);
        if (existing == null && !isBlank(code)) {
            existing = categoryRepository.findFirstByCodeIgnoreCase(code.trim()).orElse(null);
        }

        CategoryEntity parent = resolveParent(parentId, parentCode, parentName);

        if (existing != null) {
            if (parent != null && parent.getId() != null && parent.getId().equals(existing.getId())) {
                throw new CatalogRowException("Danh mục không thể là cha của chính nó");
            }
            String newCode = isBlank(code) ? existing.getCode() : code.trim().toUpperCase();
            String newName = name.trim();
            Integer newStatus = status != null ? status : existing.getStatus();
            // parent chỉ đổi khi file chỉ định (parentId/parentCode/parentName); nếu không -> giữ nguyên.
            boolean parentSpecified = parentId != null || !isBlank(parentCode) || !isBlank(parentName);
            CategoryEntity newParent = parentSpecified ? parent : existing.getParent();
            Long curParentId = existing.getParent() != null ? existing.getParent().getId() : null;
            Long newParentId = newParent != null ? newParent.getId() : null;

            // SKIP: không có thay đổi nào so với bản ghi hiện có.
            boolean unchanged = Objects.equals(trimToNull(existing.getCode()), trimToNull(newCode))
                    && Objects.equals(trimToNull(existing.getName()), trimToNull(newName))
                    && Objects.equals(existing.getStatus(), newStatus)
                    && Objects.equals(curParentId, newParentId);
            if (unchanged) {
                return new Outcome("SKIPPED", existing.getId());
            }

            if (!isBlank(code)) {
                String c = code.trim().toUpperCase();
                if (categoryRepository.existsByCodeIgnoreCaseAndIdNot(c, existing.getId())) {
                    throw new CatalogRowException("Trùng code với danh mục khác: " + c);
                }
                existing.setCode(c);
            }
            existing.setName(newName);
            if (status != null) existing.setStatus(status);
            if (parentSpecified) existing.setParent(parent);
            CategoryEntity saved = categoryRepository.save(existing);
            return new Outcome("UPDATED", saved.getId());
        }

        String c = isBlank(code) ? uniqueCategoryCode(name) : code.trim().toUpperCase();
        if (categoryRepository.existsByCodeIgnoreCase(c)) {
            throw new CatalogRowException("Code đã tồn tại: " + c);
        }
        CategoryEntity created = categoryRepository.save(CategoryEntity.builder()
                .code(c)
                .name(name.trim())
                .status(status != null ? status : SystemConstant.ACTIVE_STATUS)
                .parent(parent)
                .build());
        return new Outcome("CREATED", created.getId());
    }

    private CategoryEntity resolveParent(Long parentId, String parentCode, String parentName) {
        if (parentId != null) {
            return categoryRepository.findById(parentId)
                    .orElseThrow(() -> new CatalogRowException("Không tìm thấy danh mục cha id=" + parentId));
        }
        if (!isBlank(parentCode)) {
            return categoryRepository.findFirstByCodeIgnoreCase(parentCode.trim())
                    .orElseThrow(() -> new CatalogRowException("Không tìm thấy danh mục cha code=" + parentCode));
        }
        if (!isBlank(parentName)) {
            return categoryRepository.findFirstByNameIgnoreCase(parentName.trim()).orElse(null);
        }
        return null;
    }

    private String uniqueBrandCode(String name) {
        String base = slug(name), c = base;
        int i = 1;
        while (brandRepository.existsByCodeIgnoreCase(c)) c = base + "_" + (++i);
        return c;
    }

    private String uniqueCategoryCode(String name) {
        String base = slug(name), c = base;
        int i = 1;
        while (categoryRepository.existsByCodeIgnoreCase(c)) c = base + "_" + (++i);
        return c;
    }

    static String slug(String raw) {
        String noAccent = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        String code = noAccent.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (code.isEmpty()) code = "C" + Math.abs(raw.hashCode());
        return code.length() > 60 ? code.substring(0, 60) : code;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /** Chuẩn hóa để so sánh: null/chuỗi rỗng -> null, còn lại trả về bản đã trim. */
    private static String trimToNull(String s) { return isBlank(s) ? null : s.trim(); }
}
