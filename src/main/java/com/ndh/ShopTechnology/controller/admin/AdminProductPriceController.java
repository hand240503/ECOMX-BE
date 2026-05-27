package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.product.UpsertProductPriceRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductPriceResponse;
import com.ndh.ShopTechnology.services.product.ProductPriceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/products/{productId}/prices")
public class AdminProductPriceController {

    private final ProductPriceService priceService;

    public AdminProductPriceController(ProductPriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<ProductPriceResponse>>> list(@PathVariable Long productId) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", priceService.list(productId), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<ProductPriceResponse>> create(
            @PathVariable Long productId,
            @Valid @RequestBody UpsertProductPriceRequest request) {
        ProductPriceResponse data = priceService.create(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{priceId}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<ProductPriceResponse>> update(
            @PathVariable Long productId,
            @PathVariable long priceId,
            @Valid @RequestBody UpsertProductPriceRequest request) {
        ProductPriceResponse data = priceService.update(productId, priceId, request);
        return ResponseEntity.ok(APIResponse.of(true, "Updated", data, null, null));
    }

    @DeleteMapping("/{priceId}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRODUCT + ")")
    public ResponseEntity<APIResponse<Void>> delete(
            @PathVariable Long productId,
            @PathVariable long priceId) {
        priceService.delete(productId, priceId);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }
}
