package com.ndh.ShopTechnology.services.product.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cấu trúc trung gian khi phân tích file import: 1 sản phẩm = N biến thể = N dòng giá.
 * Package-private, chỉ dùng nội bộ cho luồng import.
 */
class ProductImportDraft {

    /** Dòng đầu tiên (1-based) của sản phẩm trong file — dùng để báo lỗi. */
    int rowNumber;

    String productName;
    String categoryRef;   // tên hoặc code danh mục
    String brandRef;      // tên hoặc code thương hiệu (có thể null)
    Long sku;
    Integer status;
    Boolean isFeatured;
    Boolean hotSale;
    String description;
    String longDescription;

    final List<VariantDraft> variants = new ArrayList<>();

    static class VariantDraft {
        int rowNumber;
        String skuCode;
        final Map<String, String> options = new LinkedHashMap<>();
        Integer onHand;
        Integer sortOrder;
        Boolean active;
        final List<PriceDraft> prices = new ArrayList<>();
    }

    static class PriceDraft {
        int rowNumber;
        String unitRef;       // tên đơn vị tính
        Double currentValue;  // giá bán hiện tại
        Double oldValue;      // giá cũ (nullable)
    }
}
