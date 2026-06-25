package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.services.promotion.ProductVolumePriceTierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bậc giá theo số lượng ở cấp SẢN PHẨM (gộp mọi biến thể).
 * Phục vụ trang quản trị gọi GET /admin/products/{productId}/volume-price-tiers.
 */
@RestController
@RequestMapping("${api.prefix}/admin/products/{productId}/volume-price-tiers")
public class AdminProductVolumePriceTierController {

    private final ProductVolumePriceTierService volumePriceTierService;

    public AdminProductVolumePriceTierController(ProductVolumePriceTierService volumePriceTierService) {
        this.volumePriceTierService = volumePriceTierService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<VolumePriceTierResponse>>> listByProduct(
            @PathVariable Long productId) {
        List<VolumePriceTierResponse> data = volumePriceTierService.listByProduct(productId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }
}
