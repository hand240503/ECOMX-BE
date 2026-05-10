package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.brand.CreateBrandRequest;
import com.ndh.ShopTechnology.dto.request.brand.UpdateBrandRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.brand.BrandResponse;
import com.ndh.ShopTechnology.services.brand.BrandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quản trị hãng / thương hiệu ({@link com.ndh.ShopTechnology.entities.product.BrandEntity}).
 * Dùng khi cấu hình sản phẩm ({@code brand_id}).
 */
@RestController
@RequestMapping("${api.prefix}/admin/brands")
public class AdminBrandController {

    private final BrandService brandService;

    public AdminBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_BRAND + ")")
    public ResponseEntity<APIResponse<List<BrandResponse>>> list() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", brandService.listAll(), null, null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_BRAND + ")")
    public ResponseEntity<APIResponse<BrandResponse>> getById(@PathVariable long id) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", brandService.getById(id), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_BRAND + ")")
    public ResponseEntity<APIResponse<BrandResponse>> create(@Valid @RequestBody CreateBrandRequest request) {
        BrandResponse data = brandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_BRAND + ")")
    public ResponseEntity<APIResponse<BrandResponse>> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateBrandRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Updated", brandService.update(id, request), null, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_BRAND + ")")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable long id) {
        brandService.delete(id);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }
}
