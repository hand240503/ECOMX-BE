package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;

import com.ndh.ShopTechnology.dto.request.inventory.InventoryAdjustRequest;
import com.ndh.ShopTechnology.dto.request.inventory.InventoryImportRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryLedgerResponse;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryStockResponse;
import com.ndh.ShopTechnology.services.inventory.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Quản lý kho hàng (admin): nhập kho, điều chỉnh/kiểm kê, xem tồn và lịch sử biến động.
 * Tồn kho được quản lý ở cấp biến thể (SKU / ProductVariant).
 */
@RestController
@RequestMapping("${api.prefix}/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    /** Danh sách tồn kho tất cả biến thể (lọc theo tên SP / SKU qua tham số q). */
    @GetMapping("/stocks")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<InventoryStockResponse>>> listStocks(
            @RequestParam(value = "q", required = false) String q) {
        List<InventoryStockResponse> data = inventoryService.listStocks(q);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /** Xem tồn hiện tại của một biến thể. */
    @GetMapping("/variants/{variantId}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<InventoryStockResponse>> getStock(@PathVariable Long variantId) {
        InventoryStockResponse data = inventoryService.getStock(variantId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    /** Lịch sử biến động kho (sổ cái) của một biến thể. */
    @GetMapping("/variants/{variantId}/ledger")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<InventoryLedgerResponse>>> getLedger(@PathVariable Long variantId) {
        List<InventoryLedgerResponse> data = inventoryService.getLedger(variantId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /** Nhập kho (phiếu nhập): cộng thêm onHand. */
    @PostMapping("/import")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<InventoryStockResponse>> importStock(
            @Valid @RequestBody InventoryImportRequest request) {
        InventoryStockResponse data = inventoryService.importStock(
                request.getVariantId(), request.getQuantity(), request.getNote());
        return ResponseEntity.ok(APIResponse.of(true, "Nhập kho thành công", data, null, null));
    }

    /** Điều chỉnh / kiểm kê: đặt thẳng onHand về giá trị mới. */
    @PostMapping("/adjust")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<InventoryStockResponse>> adjust(
            @Valid @RequestBody InventoryAdjustRequest request) {
        InventoryStockResponse data = inventoryService.adjustOnHand(
                request.getVariantId(), request.getOnHand(), request.getNote());
        return ResponseEntity.ok(APIResponse.of(true, "Điều chỉnh tồn kho thành công", data, null, null));
    }
}
