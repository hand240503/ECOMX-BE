package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.request.order.CreateOrderDetailRequest;
import com.ndh.ShopTechnology.entities.product.ProductEntity;

import java.util.List;
import java.util.Map;

/**
 * Tính đơn giá / thành tiền dòng đơn hàng với mix-and-match (bậc SL) và purchase-with-purchase.
 */
public interface PromotionPricingService {

    /**
     * Một dòng sau khi áp dụng khuyến mãi (thứ tự khớp {@code lines}).
     */
    record PricedLine(CreateOrderDetailRequest line, double unitPrice, double lineTotal) {
    }

    /**
     * @param lines     dòng trong request đặt hàng
     * @param productsById sản phẩm đã load đầy đủ (giá, …)
     */
    List<PricedLine> priceLines(List<CreateOrderDetailRequest> lines, Map<Long, ProductEntity> productsById);
}
