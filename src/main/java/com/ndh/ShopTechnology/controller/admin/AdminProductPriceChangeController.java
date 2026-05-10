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
 * Quản lý price change theo thời gian cho sản phẩm: giá gốc + giá ưu đãi.
 */
@RestController
@RequestMapping("${api.prefix}/admin/products/{productId}/price-changes")
public class AdminProductPriceChangeController {

    private final ProductPriceChangeService priceChangeService;

    public AdminProductPriceChangeController(ProductPriceChangeService priceChangeService) {
        this.priceChangeService = priceChangeService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRICE + ")")
    public ResponseEntity<APIResponse<List<ProductPriceChangeResponse>>> list(@PathVariable Long productId) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", priceChangeService.list(productId), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRICE + ")")
    public ResponseEntity<APIResponse<ProductPriceChangeResponse>> create(
            @PathVariable Long productId,
            @Valid @RequestBody UpsertPriceChangeRequest request) {
        ProductPriceChangeResponse data = priceChangeService.create(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{priceChangeId}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRICE + ")")
    public ResponseEntity<APIResponse<ProductPriceChangeResponse>> update(
            @PathVariable Long productId,
            @PathVariable long priceChangeId,
            @Valid @RequestBody UpsertPriceChangeRequest request) {
        ProductPriceChangeResponse data = priceChangeService.update(productId, priceChangeId, request);
        return ResponseEntity.ok(APIResponse.of(true, "Updated", data, null, null));
    }

    @DeleteMapping("/{priceChangeId}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRICE + ")")
    public ResponseEntity<APIResponse<Void>> delete(
            @PathVariable Long productId,
            @PathVariable long priceChangeId) {
        priceChangeService.delete(productId, priceChangeId);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }
}

