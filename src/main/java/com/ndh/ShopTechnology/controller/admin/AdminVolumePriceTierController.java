package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.promotion.VolumePriceTierItemRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.services.promotion.ProductVolumePriceTierService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mix-and-match theo **phân loại (SKU)**: bậc giá theo tổng SL đúng {@code variantId} trên đơn.
 */
@RestController
@RequestMapping("${api.prefix}/admin/products/{productId}/variants/{variantId}/volume-price-tiers")
public class AdminVolumePriceTierController {

    private final ProductVolumePriceTierService volumePriceTierService;

    public AdminVolumePriceTierController(ProductVolumePriceTierService volumePriceTierService) {
        this.volumePriceTierService = volumePriceTierService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<VolumePriceTierResponse>>> list(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        List<VolumePriceTierResponse> data = volumePriceTierService.listByVariant(productId, variantId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    @PutMapping
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<VolumePriceTierResponse>>> replace(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody List<VolumePriceTierItemRequest> tiers) {
        List<VolumePriceTierResponse> data = volumePriceTierService.replaceTiers(productId, variantId, tiers);
        return ResponseEntity.ok(APIResponse.of(true, "Volume tiers updated", data, null, null));
    }
}
