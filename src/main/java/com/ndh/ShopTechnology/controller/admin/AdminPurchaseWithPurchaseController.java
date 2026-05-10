package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.promotion.UpsertPurchaseWithPurchaseRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.promotion.PurchaseWithPurchaseOfferResponse;
import com.ndh.ShopTechnology.services.promotion.PurchaseWithPurchaseOfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Purchase-with-purchase: mua sản phẩm neo (anchor) được mua sản phẩm đi kèm (companion) với đơn giá ưu đãi.
 */
@RestController
@RequestMapping("${api.prefix}/admin/promotions/purchase-with-purchase")
public class AdminPurchaseWithPurchaseController {

    private final PurchaseWithPurchaseOfferService pwpService;

    public AdminPurchaseWithPurchaseController(PurchaseWithPurchaseOfferService pwpService) {
        this.pwpService = pwpService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRICE + ")")
    public ResponseEntity<APIResponse<List<PurchaseWithPurchaseOfferResponse>>> list() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", pwpService.listAll(), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRICE + ")")
    public ResponseEntity<APIResponse<PurchaseWithPurchaseOfferResponse>> create(
            @Valid @RequestBody UpsertPurchaseWithPurchaseRequest request) {
        PurchaseWithPurchaseOfferResponse data = pwpService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRICE + ")")
    public ResponseEntity<APIResponse<PurchaseWithPurchaseOfferResponse>> update(
            @PathVariable long id,
            @Valid @RequestBody UpsertPurchaseWithPurchaseRequest request) {
        PurchaseWithPurchaseOfferResponse data = pwpService.update(id, request);
        return ResponseEntity.ok(APIResponse.of(true, "Updated", data, null, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRICE + ")")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable long id) {
        pwpService.delete(id);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }
}
