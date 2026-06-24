package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bật cờ "nổi bật" (is_featured) hoặc "hot-sale" cho một sản phẩm — transaction RIÊNG
 * (REQUIRES_NEW) để mỗi dòng import độc lập. Tra sản phẩm theo sku (ưu tiên) rồi product_id.
 */
@Component
public class ProductFlagPersister {

    private final ProductRepository productRepository;

    public ProductFlagPersister(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public enum Flag { FEATURED, HOT_SALE }

    public static class Outcome {
        public final String action; // CREATED = vừa bật cờ | SKIPPED = đã bật từ trước
        public final Long id;
        Outcome(String action, Long id) { this.action = action; this.id = id; }
    }

    static class FlagRowException extends RuntimeException {
        FlagRowException(String m) { super(m); }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome setFlag(Long id, Long sku, Flag flag) {
        ProductEntity p = null;
        if (sku != null) p = productRepository.findFirstBySku(sku).orElse(null);
        if (p == null && id != null) p = productRepository.findById(id).orElse(null);
        if (p == null) {
            throw new FlagRowException("Không tìm thấy sản phẩm theo sku/product_id");
        }

        boolean current = flag == Flag.FEATURED
                ? Boolean.TRUE.equals(p.getIsFeatured())
                : Boolean.TRUE.equals(p.getHotSale());
        if (current) {
            return new Outcome("SKIPPED", p.getId());
        }

        if (flag == Flag.FEATURED) {
            p.setIsFeatured(true);
        } else {
            p.setHotSale(true);
        }
        productRepository.save(p);
        return new Outcome("CREATED", p.getId());
    }
}
