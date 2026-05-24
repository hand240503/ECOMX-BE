package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.request.order.CreateOrderDetailRequest;
import com.ndh.ShopTechnology.dto.response.order.CheckoutPwpSuggestionDto;
import com.ndh.ShopTechnology.dto.response.order.OrderLinePricingProgramsDto;
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

    /**
     * Giống {@link PricedLine} kèm snapshot chương trình (PC / volume tier / PWP) để preview và lưu đơn.
     */
    record PricedLineWithPrograms(
            CreateOrderDetailRequest line,
            double finalUnitPrice,
            double lineTotal,
            OrderLinePricingProgramsDto programs) {
    }

    /**
     * Kết quả tổng hợp dùng cho API checkout-pricing-preview:
     * <ul>
     *   <li>{@code pricedLines} — danh sách dòng đã áp giá (PC, volume tier, PwP nếu companion đã có trong giỏ).</li>
     *   <li>{@code pwpSuggestions} — gợi ý mua kèm PwP khi SP neo có trong giỏ nhưng companion chưa có.</li>
     * </ul>
     */
    record PricingWithSuggestionsResult(
            List<PricedLineWithPrograms> pricedLines,
            List<CheckoutPwpSuggestionDto> pwpSuggestions) {
    }

    List<PricedLine> priceLines(List<OrderVariantLine> lines);

    List<PricedLineWithPrograms> priceLinesWithPrograms(List<OrderVariantLine> lines);

    /**
     * Giống {@link #priceLinesWithPrograms} nhưng đồng thời trả về danh sách gợi ý PwP (companion chưa có
     * trong giỏ). Dùng cho API {@code POST /orders/checkout-pricing-preview}.
     */
    PricingWithSuggestionsResult priceLinesWithProgramsAndSuggestions(List<OrderVariantLine> lines);
}
