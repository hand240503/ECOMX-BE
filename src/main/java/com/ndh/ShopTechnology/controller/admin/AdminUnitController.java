package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.unit.CreateUnitRequest;
import com.ndh.ShopTechnology.dto.request.unit.UpdateUnitRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.unit.UnitResponse;
import com.ndh.ShopTechnology.services.unit.UnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quản trị đơn vị tính ({@link com.ndh.ShopTechnology.entities.product.UnitEntity}).
 * Dùng khi cấu hình giá catalog / sản phẩm (trường {@code unit_id}).
 */
@RestController
@RequestMapping("${api.prefix}/admin/units")
public class AdminUnitController {

    private final UnitService unitService;

    public AdminUnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_UNIT + ")")
    public ResponseEntity<APIResponse<List<UnitResponse>>> list() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", unitService.listAll(), null, null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_UNIT + ")")
    public ResponseEntity<APIResponse<UnitResponse>> getById(@PathVariable long id) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", unitService.getById(id), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_UNIT + ")")
    public ResponseEntity<APIResponse<UnitResponse>> create(@Valid @RequestBody CreateUnitRequest request) {
        UnitResponse data = unitService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_UNIT + ")")
    public ResponseEntity<APIResponse<UnitResponse>> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateUnitRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Updated", unitService.update(id, request), null, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_UNIT + ")")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable long id) {
        unitService.delete(id);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }
}
