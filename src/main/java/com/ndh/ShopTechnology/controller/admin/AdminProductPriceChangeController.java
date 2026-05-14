package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.product.UpsertPriceChangeRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceChangeResponse;
import com.ndh.ShopTechnology.services.product.ProductPriceChangeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Price change theo thời gian cho từng biến thể (SKU).
 */
@RestController
@RequestMapping("${api.prefix}/admin/products/{productId}/variants/{variantId}/price-changes")
public class AdminProductPriceChangeController {

    private final ProductPriceChangeService priceChangeService;

    public AdminProductPriceChangeController(ProductPriceChangeService priceChangeService) {
        this.priceChangeService = priceChangeService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRICE + ")")
    public ResponseEntity<APIResponse<List<ProductPriceChangeResponse>>> list(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        return ResponseEntity.ok(APIResponse.of(true, "OK",
                priceChangeService.list(productId, variantId), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRICE + ")")
    public ResponseEntity<APIResponse<ProductPriceChangeResponse>> create(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody UpsertPriceChangeRequest request) {
        ProductPriceChangeResponse data = priceChangeService.create(productId, variantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{priceChangeId}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRICE + ")")
    public ResponseEntity<APIResponse<ProductPriceChangeResponse>> update(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @PathVariable long priceChangeId,
            @Valid @RequestBody UpsertPriceChangeRequest request) {
        ProductPriceChangeResponse data = priceChangeService.update(productId, variantId, priceChangeId, request);
        return ResponseEntity.ok(APIResponse.of(true, "Updated", data, null, null));
    }

    @DeleteMapping("/{priceChangeId}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRICE + ")")
    public ResponseEntity<APIResponse<Void>> delete(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @PathVariable long priceChangeId) {
        priceChangeService.delete(productId, variantId, priceChangeId);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }
}
