package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceChangeResponse;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.services.product.ProductPriceChangeService;
import com.ndh.ShopTechnology.services.promotion.ProductVolumePriceTierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Liệt kê TẤT CẢ sản phẩm/biến thể đang cấu hình chương trình giá để trang quản trị
 * hiển thị tổng quan ngay khi truy cập:
 * - Giá theo khung thời gian (Price Change)
 * - Giá theo bậc số lượng (Mix &amp; Match / Volume price tier)
 *
 * <p>Mua kèm (PwP) đã có endpoint liệt kê tất cả tại
 * {@code /admin/promotions/purchase-with-purchase}.
 */
@RestController
@RequestMapping("${api.prefix}/admin/promotions")
public class AdminPromotionOverviewController {

    private final ProductPriceChangeService priceChangeService;
    private final ProductVolumePriceTierService volumeTierService;

    public AdminPromotionOverviewController(ProductPriceChangeService priceChangeService,
                                            ProductVolumePriceTierService volumeTierService) {
        this.priceChangeService = priceChangeService;
        this.volumeTierService = volumeTierService;
    }

    /** Tất cả chương trình đổi giá theo thời gian (mọi sản phẩm/biến thể). */
    @GetMapping("/price-changes")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<ProductPriceChangeResponse>>> listAllPriceChanges() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", priceChangeService.listAll(), null, null));
    }

    /** Tất cả bậc giá theo số lượng (mọi sản phẩm/biến thể). */
    @GetMapping("/volume-price-tiers")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<VolumePriceTierResponse>>> listAllVolumeTiers() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", volumeTierService.listAll(), null, null));
    }
}
