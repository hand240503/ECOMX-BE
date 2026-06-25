package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lưu một biến thể import trong transaction RIÊNG (REQUIRES_NEW) để mỗi biến thể độc lập —
 * biến thể lỗi (vd trùng sku_code) rollback riêng, không ảnh hưởng các dòng hợp lệ khác.
 *
 * <p>CHỈ ghi bảng product_variant (sku_code, option_values, sort_order, active).
 * KHÔNG đụng tới giá, tồn kho, danh mục, thương hiệu.
 */
@Component
public class ProductVariantImportPersister {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public ProductVariantImportPersister(ProductRepository productRepository,
                                         ProductVariantRepository variantRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
    }

    static class PersistResult {
        Long variantId;
        boolean updated; // true = cập nhật biến thể đã có; false = tạo mới
        boolean skipped; // true = không có thay đổi -> bỏ qua
    }

    /** Tạo biến thể mới cho sản phẩm. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistResult create(Long productId, VariantImportDraft d) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ImportRowException("Không tìm thấy sản phẩm (id=" + productId + ")"));

        if (d.skuCode != null && !d.skuCode.isBlank()) {
            assertSkuCodeFree(d.skuCode.trim(), null);
        }

        int nextSort = d.sortOrder != null ? d.sortOrder : (int) variantRepository.countByProduct_Id(productId);
        ProductVariantEntity v = ProductVariantEntity.builder()
                .product(product)
                .skuCode(isBlank(d.skuCode) ? null : d.skuCode.trim())
                .optionValues(new LinkedHashMap<>(d.options))
                .active(d.active != null ? d.active : true)
                .sortOrder(nextSort)
                .onHand(0)
                .reserved(0)
                .build();
        ProductVariantEntity saved = variantRepository.save(v);

        PersistResult r = new PersistResult();
        r.variantId = saved.getId();
        r.updated = false;
        return r;
    }

    /** Cập nhật một biến thể đã có (chỉ option_values, sort_order, active; sku_code có thể đổi nếu chưa bị chiếm). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistResult update(Long productId, Long variantId, VariantImportDraft d) {
        ProductVariantEntity v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ImportRowException("Không tìm thấy biến thể để cập nhật (id=" + variantId + ")"));
        if (v.getProduct() == null || !productId.equals(v.getProduct().getId())) {
            throw new ImportRowException("Biến thể không thuộc sản phẩm này");
        }

        boolean changed = false;
        if (!isBlank(d.skuCode) && !d.skuCode.trim().equalsIgnoreCase(v.getSkuCode())) {
            assertSkuCodeFree(d.skuCode.trim(), variantId);
            v.setSkuCode(d.skuCode.trim());
            changed = true;
        }
        if (d.options != null && !d.options.isEmpty() && !d.options.equals(v.getOptionValues())) {
            v.setOptionValues(new LinkedHashMap<>(d.options));
            changed = true;
        }
        if (d.sortOrder != null && !d.sortOrder.equals(v.getSortOrder())) {
            v.setSortOrder(d.sortOrder);
            changed = true;
        }
        if (d.active != null && !d.active.equals(v.getActive())) {
            v.setActive(d.active);
            changed = true;
        }

        PersistResult r = new PersistResult();
        r.variantId = v.getId();
        if (!changed) {
            r.skipped = true;
            r.updated = false;
            return r;
        }
        variantRepository.save(v);
        r.updated = true;
        return r;
    }

    private void assertSkuCodeFree(String skuCode, Long ignoreVariantId) {
        List<ProductVariantEntity> hits = variantRepository.findBySkuCodeIgnoreCaseFetchProduct(skuCode);
        for (ProductVariantEntity hit : hits) {
            if (ignoreVariantId == null || !ignoreVariantId.equals(hit.getId())) {
                throw new ImportRowException("Mã SKU biến thể '" + skuCode + "' đã được dùng cho biến thể khác");
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Lỗi cấp một biến thể — impl bắt lại để báo cáo và bỏ qua dòng đó. */
    static class ImportRowException extends RuntimeException {
        ImportRowException(String message) {
            super(message);
        }
    }

    /** Cấu trúc trung gian một biến thể đọc từ file. */
    static class VariantImportDraft {
        int rowNumber;
        String skuCode;
        final Map<String, String> options = new LinkedHashMap<>();
        Integer sortOrder;
        Boolean active;
    }
}
