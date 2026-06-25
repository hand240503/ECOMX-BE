package com.ndh.ShopTechnology.services.product.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cấu trúc trung gian khi phân tích file import sản phẩm: 1 sản phẩm = N biến thể.
 *
 * <p>CHỈ chứa thông tin thuộc bảng product + product_variant. KHÔNG mang theo
 * danh mục / thương hiệu / đơn vị / giá — các bảng đó do chức năng riêng quản lý
 * (gán danh mục/thương hiệu hàng loạt, import giá). Package-private, dùng nội bộ.
 */
class ProductImportDraft {

    /** Dòng đầu tiên (1-based) của sản phẩm trong file — dùng để báo lỗi. */
    int rowNumber;

    String productName;
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
        Integer sortOrder;
        Boolean active;
    }
}
