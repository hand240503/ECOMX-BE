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
 * Mix-and-match: bậc giá theo tổng số lượng sản phẩm trên đơn (ví dụ 1 cái 100k, 2 cái 150k).
 */
@RestController
@RequestMapping("${api.prefix}/admin/products/{productId}/volume-price-tiers")
public class AdminVolumePriceTierController {

    private final ProductVolumePriceTierService volumePriceTierService;

    public AdminVolumePriceTierController(ProductVolumePriceTierService volumePriceTierService) {
        this.volumePriceTierService = volumePriceTierService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRICE + ")")
    public ResponseEntity<APIResponse<List<VolumePriceTierResponse>>> list(@PathVariable Long productId) {
        List<VolumePriceTierResponse> data = volumePriceTierService.listByProductId(productId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    /**
     * Thay toàn bộ bậc giá của sản phẩm (body rỗng = xóa hết bậc, chỉ còn giá catalog).
     *
     * <p>PUT replace = vừa create vừa delete tier ⇒ yêu cầu cả {@code CREATE_PRICE}
     * và {@code DELETE_PRICE} (kèm {@code UPDATE_PRICE}) sẽ chặt chẽ; nhưng để FE
     * dễ giao quyền, chỉ check {@code UPDATE_PRICE} (semantic "update toàn bộ tier list").
     */
    @PutMapping
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRICE + ")")
    public ResponseEntity<APIResponse<List<VolumePriceTierResponse>>> replace(
            @PathVariable Long productId,
            @Valid @RequestBody List<VolumePriceTierItemRequest> tiers) {
        List<VolumePriceTierResponse> data = volumePriceTierService.replaceTiers(productId, tiers);
        return ResponseEntity.ok(APIResponse.of(true, "Volume tiers updated", data, null, null));
    }
}
