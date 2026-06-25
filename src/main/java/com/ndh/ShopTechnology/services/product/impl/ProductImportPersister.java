package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

/**
 * Lưu một sản phẩm import trong transaction RIÊNG (REQUIRES_NEW) để mỗi sản phẩm
 * độc lập — sản phẩm lỗi rollback riêng, không ảnh hưởng các sản phẩm hợp lệ khác.
 *
 * <p>NGUYÊN TẮC: chức năng "Tải sản phẩm lên" CHỈ ghi bảng product + product_variant.
 * KHÔNG tạo/sửa danh mục, thương hiệu, đơn vị tính; KHÔNG ghi/sửa bảng giá.
 * <ul>
 *   <li>Danh mục / thương hiệu: gán sau bằng chức năng "Gán danh mục/thương hiệu hàng loạt".</li>
 *   <li>Giá + đơn vị: nhập bằng chức năng quản lý giá riêng.</li>
 * </ul>
 * Sản phẩm mới được tạo với category = null (cột category_id đã cho phép NULL).
 */
@Component
public class ProductImportPersister {

    private final ProductRepository productRepository;

    public ProductImportPersister(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Kết quả lưu một sản phẩm. */
    static class PersistResult {
        Long productId;
        int variantCount;
        boolean updated; // true = cập nhật sản phẩm đã có; false = tạo mới
        boolean skipped; // true = đã tồn tại nhưng không có thay đổi -> bỏ qua, không ghi
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistResult persist(ProductImportDraft d) {
        if (isBlank(d.productName)) {
            throw new ImportRowException("Thiếu tên sản phẩm");
        }

        ProductEntity product = ProductEntity.builder()
                .productName(d.productName.trim())
                .description(d.description)
                .lDescription(d.longDescription)
                .status(d.status != null ? d.status : SystemConstant.ACTIVE_STATUS)
                .sku(d.sku)
                .isFeatured(d.isFeatured != null ? d.isFeatured : false)
                .hotSale(d.hotSale != null ? d.hotSale : false)
                // category & brand cố tình để null — gán sau bằng chức năng riêng.
                .build();

        int variantCount = 0;
        for (ProductImportDraft.VariantDraft vd : d.variants) {
            ProductVariantEntity v = ProductVariantEntity.builder()
                    .product(product)
                    .skuCode(isBlank(vd.skuCode) ? null : vd.skuCode.trim())
                    .optionValues(new LinkedHashMap<>(vd.options))
                    .active(vd.active != null ? vd.active : true)
                    .sortOrder(vd.sortOrder != null ? vd.sortOrder : variantCount)
                    .onHand(0)
                    .reserved(0)
                    .build();
            // Giá KHÔNG tạo ở đây — nhập bằng chức năng quản lý giá riêng.
            product.getVariants().add(v);
            variantCount++;
        }

        ProductEntity saved = productRepository.save(product);
        PersistResult r = new PersistResult();
        r.productId = saved.getId();
        r.variantCount = variantCount;
        r.updated = false;
        return r;
    }

    /**
     * Cập nhật một sản phẩm đã tồn tại — chạy transaction RIÊNG.
     * CHỈ cập nhật cột bảng product (khi file có giá trị) và thuộc tính RIÊNG của biến thể
     * (sku_code, option_values, sort_order, active). KHÔNG đụng tới danh mục, thương hiệu,
     * giá hay tồn kho. Biến thể vắng mặt trong file KHÔNG bị xóa.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistResult update(Long productId, ProductImportDraft d) {
        ProductEntity product = productRepository.findWithFullRelationsById(productId)
                .orElseThrow(() -> new ImportRowException("Không tìm thấy sản phẩm để cập nhật (id=" + productId + ")"));

        boolean changed = false;

        if (!isBlank(d.productName) && !d.productName.trim().equals(product.getProductName())) {
            product.setProductName(d.productName.trim()); changed = true;
        }
        if (d.sku != null && !d.sku.equals(product.getSku())) { product.setSku(d.sku); changed = true; }
        if (d.status != null && !d.status.equals(product.getStatus())) { product.setStatus(d.status); changed = true; }
        if (d.isFeatured != null && !d.isFeatured.equals(product.getIsFeatured())) {
            product.setIsFeatured(d.isFeatured); changed = true;
        }
        if (d.hotSale != null && !d.hotSale.equals(product.getHotSale())) {
            product.setHotSale(d.hotSale); changed = true;
        }
        if (d.description != null && !d.description.equals(product.getDescription())) {
            product.setDescription(d.description); changed = true;
        }
        if (d.longDescription != null && !d.longDescription.equals(product.getLDescription())) {
            product.setLDescription(d.longDescription); changed = true;
        }

        int variantCount = 0;
        for (ProductImportDraft.VariantDraft vd : d.variants) {
            ProductVariantEntity v = findVariant(product, vd);
            if (v == null) {
                v = ProductVariantEntity.builder()
                        .product(product)
                        .skuCode(isBlank(vd.skuCode) ? null : vd.skuCode.trim())
                        .optionValues(new LinkedHashMap<>(vd.options))
                        .active(vd.active != null ? vd.active : true)
                        .sortOrder(vd.sortOrder != null ? vd.sortOrder : product.getVariants().size())
                        .onHand(0)
                        .reserved(0)
                        .build();
                product.getVariants().add(v);
                changed = true;
            } else {
                if (!isBlank(vd.skuCode) && !vd.skuCode.trim().equalsIgnoreCase(v.getSkuCode())) {
                    v.setSkuCode(vd.skuCode.trim()); changed = true;
                }
                if (vd.options != null && !vd.options.isEmpty() && !vd.options.equals(v.getOptionValues())) {
                    v.setOptionValues(new LinkedHashMap<>(vd.options)); changed = true;
                }
                if (vd.sortOrder != null && !vd.sortOrder.equals(v.getSortOrder())) {
                    v.setSortOrder(vd.sortOrder); changed = true;
                }
                if (vd.active != null && !vd.active.equals(v.getActive())) {
                    v.setActive(vd.active); changed = true;
                }
            }
            variantCount++;
        }

        PersistResult r = new PersistResult();
        r.variantCount = variantCount;
        if (!changed) {
            r.productId = product.getId();
            r.updated = false;
            r.skipped = true;
            return r;
        }
        ProductEntity saved = productRepository.save(product);
        r.productId = saved.getId();
        r.updated = true;
        return r;
    }

    /** Tìm biến thể hiện có khớp theo sku_code (ưu tiên), nếu không có sku_code thì khớp theo bộ option. */
    private ProductVariantEntity findVariant(ProductEntity product, ProductImportDraft.VariantDraft vd) {
        if (!isBlank(vd.skuCode)) {
            for (ProductVariantEntity v : product.getVariants()) {
                if (v.getSkuCode() != null && v.getSkuCode().equalsIgnoreCase(vd.skuCode.trim())) return v;
            }
            return null; // có sku_code nhưng chưa khớp -> biến thể mới
        }
        if (vd.options != null && !vd.options.isEmpty()) {
            for (ProductVariantEntity v : product.getVariants()) {
                if (v.getOptionValues() != null && v.getOptionValues().equals(vd.options)) return v;
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Lỗi cấp một sản phẩm — impl bắt lại để báo cáo và bỏ qua sản phẩm đó. */
    static class ImportRowException extends RuntimeException {
        ImportRowException(String message) {
            super(message);
        }
    }
}
