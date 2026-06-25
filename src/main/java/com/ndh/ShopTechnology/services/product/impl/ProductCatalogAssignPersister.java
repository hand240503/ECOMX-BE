package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Gán liên kết product → danh mục / thương hiệu trong transaction RIÊNG (REQUIRES_NEW),
 * mỗi dòng độc lập. CHỈ thay đổi cột category_id / brand_id của sản phẩm; tra brand/category
 * theo code (PHẢI tồn tại sẵn — không tự tạo). Cột để trống = giữ nguyên liên kết hiện tại.
 */
@Component
public class ProductCatalogAssignPersister {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ProductCatalogAssignPersister(ProductRepository productRepository,
                                         BrandRepository brandRepository,
                                         CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    public static class Outcome {
        public final String action; // UPDATED = vừa đổi liên kết | SKIPPED = không có gì đổi
        public final Long id;
        Outcome(String action, Long id) { this.action = action; this.id = id; }
    }

    static class AssignRowException extends RuntimeException {
        AssignRowException(String m) { super(m); }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome assign(Long sku, Long productId, String brandCode, String categoryCode) {
        ProductEntity p = null;
        if (sku != null) p = productRepository.findFirstBySku(sku).orElse(null);
        if (p == null && productId != null) p = productRepository.findById(productId).orElse(null);
        if (p == null) {
            throw new AssignRowException("Không tìm thấy sản phẩm theo sku/product_id");
        }

        boolean changed = false;

        if (!isBlank(categoryCode)) {
            CategoryEntity cat = categoryRepository.findFirstByCodeIgnoreCase(categoryCode.trim())
                    .orElseThrow(() -> new AssignRowException("Không tìm thấy danh mục code=" + categoryCode));
            Long cur = p.getCategory() != null ? p.getCategory().getId() : null;
            if (!Objects.equals(cur, cat.getId())) { p.setCategory(cat); changed = true; }
        }

        if (!isBlank(brandCode)) {
            BrandEntity br = brandRepository.findFirstByCodeIgnoreCase(brandCode.trim())
                    .orElseThrow(() -> new AssignRowException("Không tìm thấy thương hiệu code=" + brandCode));
            Long cur = p.getBrand() != null ? p.getBrand().getId() : null;
            if (!Objects.equals(cur, br.getId())) { p.setBrand(br); changed = true; }
        }

        if (!changed) {
            return new Outcome("SKIPPED", p.getId());
        }
        productRepository.save(p);
        return new Outcome("UPDATED", p.getId());
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
