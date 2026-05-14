package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.request.order.CreateOrderDetailRequest;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;

import java.util.List;

/**
 * Tính đơn giá / thành tiền dòng đơn hàng với mix-and-match (bậc SL) và purchase-with-purchase.
 */
public interface PromotionPricingService {

    /**
     * Một dòng đặt hàng đã gắn biến thể (SKU) để tính giá.
     */
    record OrderVariantLine(CreateOrderDetailRequest line, ProductVariantEntity variant) {
    }

    /**
     * Một dòng sau khi áp dụng khuyến mãi (thứ tự khớp {@code lines}).
     */
    record PricedLine(CreateOrderDetailRequest line, double unitPrice, double lineTotal) {
    }

    List<PricedLine> priceLines(List<OrderVariantLine> lines);
}
