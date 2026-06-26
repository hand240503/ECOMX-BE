package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.store.StockTransferRequest;
import com.ndh.ShopTechnology.dto.request.store.StoreCreateRequest;
import com.ndh.ShopTechnology.dto.request.store.StoreUpdateRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.store.StoreResponse;
import com.ndh.ShopTechnology.services.inventory.InventoryService;
import com.ndh.ShopTechnology.services.store.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Quản lý kho / cửa hàng (admin): thêm/sửa/xoá kho và chuyển hàng giữa các kho.
 * Yêu cầu quyền MODULE_STORE (250xxx).
 */
@RestController
@RequestMapping("${api.prefix}/admin/stores")
@RequiredArgsConstructor
public class AdminStoreController {

    private final StoreService storeService;
    private final InventoryService inventoryService;

    /** Danh sách kho (lọc theo tên / mã / thành phố qua q). */
    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_STORE + ")")
    public ResponseEntity<APIResponse<List<StoreResponse>>> list(
            @RequestParam(value = "q", required = false) String q) {
        List<StoreResponse> data = storeService.list(q);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /** Chi tiết một kho. */
    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_STORE + ")")
    public ResponseEntity<APIResponse<StoreResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", storeService.get(id), null, null));
    }

    /** Tạo kho mới. */
    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_STORE + ")")
    public ResponseEntity<APIResponse<StoreResponse>> create(@Valid @RequestBody StoreCreateRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Tạo kho thành công", storeService.create(request), null, null));
    }

    /** Cập nhật kho. */
    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_STORE + ")")
    public ResponseEntity<APIResponse<StoreResponse>> update(
            @PathVariable Long id, @Valid @RequestBody StoreUpdateRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Cập nhật kho thành công", storeService.update(id, request), null, null));
    }

    /** Xoá kho (chỉ khi không còn tồn và không gắn đơn). */
    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_STORE + ")")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ResponseEntity.ok(APIResponse.of(true, "Đã xoá kho", null, null, null));
    }

    /** Chuyển hàng từ kho này sang kho khác. */
    @PostMapping("/transfer")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_STORE + ")")
    public ResponseEntity<APIResponse<Void>> transfer(@Valid @RequestBody StockTransferRequest request) {
        inventoryService.transfer(request);
        return ResponseEntity.ok(APIResponse.of(true, "Chuyển kho thành công", null, null, null));
    }
}
