package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.request.order.CreateOrderDetailRequest;
import com.ndh.ShopTechnology.dto.response.order.CheckoutPwpSuggestionDto;
import com.ndh.ShopTechnology.dto.response.order.OrderLinePricingProgramsDto;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;

import java.util.List;

public interface PromotionPricingService {

    record OrderVariantLine(CreateOrderDetailRequest line, ProductVariantEntity variant) {
    }

    record PricedLine(CreateOrderDetailRequest line, double unitPrice, double lineTotal) {
    }

    record PricedLineWithPrograms(
            CreateOrderDetailRequest line,
            double finalUnitPrice,
            double lineTotal,
            OrderLinePricingProgramsDto programs) {
    }

    record PricingWithSuggestionsResult(
            List<PricedLineWithPrograms> pricedLines,
            List<CheckoutPwpSuggestionDto> pwpSuggestions) {
    }

    record PricingContext(String paymentMethodCode) {
        public static final PricingContext UNKNOWN = new PricingContext(null);
    }

    List<PricedLine> priceLines(List<OrderVariantLine> lines);

    List<PricedLineWithPrograms> priceLinesWithPrograms(List<OrderVariantLine> lines);

    List<PricedLineWithPrograms> priceLinesWithPrograms(List<OrderVariantLine> lines, PricingContext context);

    PricingWithSuggestionsResult priceLinesWithProgramsAndSuggestions(List<OrderVariantLine> lines);

    PricingWithSuggestionsResult priceLinesWithProgramsAndSuggestions(List<OrderVariantLine> lines, PricingContext context);
}
