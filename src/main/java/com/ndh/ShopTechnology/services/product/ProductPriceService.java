package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.request.product.UpsertProductPriceRequest;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceResponse;

import java.util.List;

/**
 * Quản trị giá catalog hằng ngày của sản phẩm (bảng {@code price}).
 *
 * <p>Đây là "giá nền" / fallback của hệ thống tính tiền (xem
 * {@link com.ndh.ShopTechnology.utils.CatalogProductUnitPrice}). Khi không có
 * price change theo thời gian / volume tier / PwP, đơn hàng sẽ áp giá catalog này.
 */
public interface ProductPriceService {

    /** Liệt kê toàn bộ entry giá catalog (theo thứ tự id tăng dần) của 1 sản phẩm. */
    List<ProductPriceResponse> list(Long productId);

    /** Tạo entry giá mới (gắn cùng product + unit). */
    ProductPriceResponse create(Long productId, UpsertProductPriceRequest request);

    /** Cập nhật currentValue / oldValue / unit của entry. */
    ProductPriceResponse update(Long productId, long priceId, UpsertProductPriceRequest request);

    /**
     * Xóa entry giá. Nếu sau khi xóa sản phẩm không còn entry giá nào, các flow
     * tính tiền dùng {@link com.ndh.ShopTechnology.utils.CatalogProductUnitPrice}
     * sẽ ném 400 — tài liệu FE cần cảnh báo điều này.
     */
    void delete(Long productId, long priceId);
}
