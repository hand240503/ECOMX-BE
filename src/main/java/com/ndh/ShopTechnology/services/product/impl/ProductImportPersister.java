package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.UnitRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Lưu một sản phẩm import trong transaction RIÊNG (REQUIRES_NEW) để mỗi sản phẩm
 * độc lập — sản phẩm lỗi rollback riêng, không ảnh hưởng các sản phẩm hợp lệ khác.
 *
 * <p>Tự động tạo Danh mục / Thương hiệu / Đơn vị tính nếu chưa tồn tại (tra theo
 * code rồi tới tên, không phân biệt hoa thường).
 */
@Component
public class ProductImportPersister {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UnitRepository unitRepository;

    public ProductImportPersister(ProductRepository productRepository,
                                  CategoryRepository categoryRepository,
                                  BrandRepository brandRepository,
                                  UnitRepository unitRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.unitRepository = unitRepository;
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
        if (isBlank(d.categoryRef)) {
            throw new ImportRowException("Thiếu danh mục");
        }

        CategoryEntity category = resolveCategory(d.categoryRef.trim());
        BrandEntity brand = isBlank(d.brandRef) ? null : resolveBrand(d.brandRef.trim());

        ProductEntity product = ProductEntity.builder()
                .productName(d.productName.trim())
                .description(d.description)
                .lDescription(d.longDescription)
                .status(d.status != null ? d.status : SystemConstant.ACTIVE_STATUS)
                .sku(d.sku)
                .isFeatured(d.isFeatured != null ? d.isFeatured : false)
                .hotSale(d.hotSale != null ? d.hotSale : false)
                .category(category)
                .brand(brand)
                .build();

        if (d.variants.isEmpty()) {
            throw new ImportRowException("Sản phẩm không có biến thể nào");
        }

        int variantCount = 0;
        for (ProductImportDraft.VariantDraft vd : d.variants) {
            ProductVariantEntity v = ProductVariantEntity.builder()
                    .product(product)
                    .skuCode(isBlank(vd.skuCode) ? null : vd.skuCode.trim())
                    .optionValues(new LinkedHashMap<>(vd.options))
                    .active(vd.active != null ? vd.active : true)
                    .sortOrder(vd.sortOrder != null ? vd.sortOrder : variantCount)
                    .onHand(vd.onHand != null && vd.onHand >= 0 ? vd.onHand : 0)
                    .reserved(0)
                    .build();

            for (ProductImportDraft.PriceDraft pd : vd.prices) {
                if (isBlank(pd.unitRef)) {
                    throw new ImportRowException("Dòng " + pd.rowNumber + ": có giá nhưng thiếu đơn vị tính");
                }
                if (pd.currentValue == null) {
                    throw new ImportRowException("Dòng " + pd.rowNumber + ": thiếu giá bán (current_value)");
                }
                if (pd.currentValue < 0) {
                    throw new ImportRowException("Dòng " + pd.rowNumber + ": giá bán không hợp lệ");
                }
                UnitEntity unit = resolveUnit(pd.unitRef.trim());
                PriceEntity price = PriceEntity.builder()
                        .unit(unit)
                        .variant(v)
                        .currentValue(pd.currentValue)
                        .oldValue(pd.oldValue != null ? pd.oldValue : 0.0)
                        .build();
                v.getPrices().add(price);
            }

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
     * Cập nhật một sản phẩm đã tồn tại (upsert) — chạy transaction RIÊNG.
     * Cập nhật các trường sản phẩm (chỉ ghi đè khi file có giá trị); biến thể & giá được
     * gộp theo sku_code / đơn vị (thêm mới hoặc cập nhật), KHÔNG xóa biến thể/giá cũ vắng mặt.
     * Tồn kho (onHand) KHÔNG đụng tới (quản lý ở chức năng kho).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistResult update(Long productId, ProductImportDraft d) {
        ProductEntity product = productRepository.findWithFullRelationsById(productId)
                .orElseThrow(() -> new ImportRowException("Không tìm thấy sản phẩm để cập nhật (id=" + productId + ")"));

        // Theo dõi có thay đổi thực sự nào không; nếu không -> SKIP (không ghi đè, log sạch).
        boolean changed = false;

        if (!isBlank(d.productName) && !d.productName.trim().equals(product.getProductName())) {
            product.setProductName(d.productName.trim()); changed = true;
        }
        if (!isBlank(d.categoryRef)) {
            CategoryEntity cat = resolveCategory(d.categoryRef.trim());
            if (!sameId(product.getCategory() != null ? product.getCategory().getId() : null,
                    cat != null ? cat.getId() : null)) {
                product.setCategory(cat); changed = true;
            }
        }
        if (!isBlank(d.brandRef)) {
            BrandEntity br = resolveBrand(d.brandRef.trim());
            if (!sameId(product.getBrand() != null ? product.getBrand().getId() : null,
                    br != null ? br.getId() : null)) {
                product.setBrand(br); changed = true;
            }
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

            for (ProductImportDraft.PriceDraft pd : vd.prices) {
                if (isBlank(pd.unitRef)) {
                    throw new ImportRowException("Dòng " + pd.rowNumber + ": có giá nhưng thiếu đơn vị tính");
                }
                if (pd.currentValue == null) {
                    throw new ImportRowException("Dòng " + pd.rowNumber + ": thiếu giá bán (current_value)");
                }
                if (pd.currentValue < 0) {
                    throw new ImportRowException("Dòng " + pd.rowNumber + ": giá bán không hợp lệ");
                }
                UnitEntity unit = resolveUnit(pd.unitRef.trim());
                PriceEntity price = findPrice(v, unit);
                if (price == null) {
                    price = PriceEntity.builder()
                            .unit(unit).variant(v)
                            .currentValue(pd.currentValue)
                            .oldValue(pd.oldValue != null ? pd.oldValue : 0.0)
                            .build();
                    v.getPrices().add(price);
                    changed = true;
                } else {
                    if (!Objects.equals(pd.currentValue, price.getCurrentValue())) {
                        price.setCurrentValue(pd.currentValue); changed = true;
                    }
                    if (pd.oldValue != null && !Objects.equals(pd.oldValue, price.getOldValue())) {
                        price.setOldValue(pd.oldValue); changed = true;
                    }
                }
            }
            variantCount++;
        }

        PersistResult r = new PersistResult();
        r.variantCount = variantCount;
        if (!changed) {
            // Không có gì đổi -> bỏ qua, không gọi save (tránh bump modified_date vô nghĩa).
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

    private static boolean sameId(Long a, Long b) { return Objects.equals(a, b); }

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

    private PriceEntity findPrice(ProductVariantEntity v, UnitEntity unit) {
        for (PriceEntity p : v.getPrices()) {
            if (p.getUnit() != null && unit != null && unit.getId() != null
                    && unit.getId().equals(p.getUnit().getId())) {
                return p;
            }
        }
        return null;
    }

    // ── Resolve helpers (auto-create nếu chưa có) ────────────────────────────

    private CategoryEntity resolveCategory(String ref) {
        return categoryRepository.findByCode(ref.toUpperCase())
                .or(() -> categoryRepository.findFirstByNameIgnoreCase(ref))
                .orElseGet(() -> categoryRepository.save(CategoryEntity.builder()
                        .name(ref)
                        .code(uniqueCategoryCode(ref))
                        .status(SystemConstant.ACTIVE_STATUS)
                        .build()));    }

    private BrandEntity resolveBrand(String ref) {
        return brandRepository.findFirstByCodeIgnoreCase(ref)
                .or(() -> brandRepository.findFirstByNameIgnoreCase(ref))
                .orElseGet(() -> brandRepository.save(BrandEntity.builder()
                        .name(ref)
                        .code(uniqueBrandCode(ref))
                        .status(SystemConstant.ACTIVE_STATUS)
                        .build()));
    }

    private UnitEntity resolveUnit(String ref) {
        return unitRepository.findFirstByNameUnitIgnoreCase(ref)
                .orElseGet(() -> unitRepository.save(UnitEntity.builder()
                        .nameUnit(ref)
                        .ratio(1)
                        .status(SystemConstant.ACTIVE_STATUS)
                        .build()));
    }

    private String uniqueCategoryCode(String name) {
        String base = slug(name);
        String code = base;
        int i = 1;
        while (categoryRepository.existsByCode(code)) {
            code = base + "_" + (++i);
        }
        return code;
    }

    private String uniqueBrandCode(String name) {
        String base = slug(name);
        String code = base;
        int i = 1;
        while (brandRepository.existsByCodeIgnoreCase(code)) {
            code = base + "_" + (++i);
        }
        return code;
    }

    /** Bỏ dấu tiếng Việt, viết hoa, thay ký tự lạ bằng '_'. */
    static String slug(String raw) {
        String noAccent = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        String code = noAccent.trim().toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (code.isEmpty()) {
            code = "C" + Math.abs(raw.hashCode());
        }
        if (code.length() > 60) {
            code = code.substring(0, 60);
        }
        return code;
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

