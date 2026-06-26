package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;

import com.ndh.ShopTechnology.dto.request.inventory.InventoryAdjustRequest;
import com.ndh.ShopTechnology.dto.request.inventory.InventoryImportRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryLedgerResponse;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryStockResponse;
import com.ndh.ShopTechnology.services.inventory.InventoryImportService;
import com.ndh.ShopTechnology.services.inventory.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Quản lý tồn kho ĐA KHO (admin): nhập kho, điều chỉnh/kiểm kê, xem tồn và lịch sử
 * biến động — tất cả theo từng kho (store). Tồn được quản lý ở cấp (kho × biến thể).
 */
@RestController
@RequestMapping("${api.prefix}/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final InventoryService inventoryService;
    private final InventoryImportService inventoryImportService;

    /** Danh sách tồn kho tất cả biến thể trong MỘT kho (lọc theo tên SP / SKU qua q). */
    @GetMapping("/stocks")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_STORE + ")")
    public ResponseEntity<APIResponse<List<InventoryStockResponse>>> listStocks(
            @RequestParam("storeId") Long storeId,
            @RequestParam(value = "q", required = false) String q) {
        List<InventoryStockResponse> data = inventoryService.listStocks(storeId, q);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /** Tồn của một biến thể tại tất cả các kho. */
    @GetMapping("/variants/{variantId}/stores")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_STORE + ")")
    public ResponseEntity<APIResponse<List<InventoryStockResponse>>> listStocksByVariant(
            @PathVariable Long variantId) {
        List<InventoryStockResponse> data = inventoryService.listStocksByVariant(variantId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /** Xem tồn hiện tại của một biến thể tại một kho. */
    @GetMapping("/stores/{storeId}/variants/{variantId}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_STORE + ")")
    public ResponseEntity<APIResponse<InventoryStockResponse>> getStock(
            @PathVariable Long storeId, @PathVariable Long variantId) {
        InventoryStockResponse data = inventoryService.getStock(storeId, variantId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    /** Lịch sử biến động kho (sổ cái) của một biến thể tại một kho. */
    @GetMapping("/stores/{storeId}/variants/{variantId}/ledger")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_STORE + ")")
    public ResponseEntity<APIResponse<List<InventoryLedgerResponse>>> getLedger(
            @PathVariable Long storeId, @PathVariable Long variantId) {
        List<InventoryLedgerResponse> data = inventoryService.getLedger(storeId, variantId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, Map.of("count", data.size())));
    }

    /** Nhập kho (phiếu nhập) vào một kho: cộng thêm onHand. */
    @PostMapping("/import")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_STORE + ")")
    public ResponseEntity<APIResponse<InventoryStockResponse>> importStock(
            @Valid @RequestBody InventoryImportRequest request) {
        InventoryStockResponse data = inventoryService.importStock(
                request.getStoreId(), request.getVariantId(), request.getQuantity(), request.getNote());
        return ResponseEntity.ok(APIResponse.of(true, "Nhập kho thành công", data, null, null));
    }

    /** Điều chỉnh / kiểm kê: đặt thẳng onHand của biến thể tại một kho. */
    @PostMapping("/adjust")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_STORE + ")")
    public ResponseEntity<APIResponse<InventoryStockResponse>> adjust(
            @Valid @RequestBody InventoryAdjustRequest request) {
        InventoryStockResponse data = inventoryService.adjustOnHand(
                request.getStoreId(), request.getVariantId(), request.getOnHand(), request.getNote());
        return ResponseEntity.ok(APIResponse.of(true, "Điều chỉnh tồn kho thành công", data, null, null));
    }

    /**
     * Import tồn kho hàng loạt vào MỘT kho từ file Excel/CSV/TXT (multipart 'file').
     * Cột 'mode': add = cộng thêm (nhập kho), set = đặt tuyệt đối (kiểm kê). Mỗi dòng độc lập.
     */
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_STORE + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> importStockExcel(
            @RequestParam("storeId") Long storeId,
            @RequestParam("file") MultipartFile file) {
        CatalogImportResponse result = inventoryImportService.importStock(storeId, file);
        String msg = String.format("Nhập thêm %d, kiểm kê %d, lỗi %d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    /** Tải file Excel mẫu để nhập tồn kho. */
    @GetMapping("/import/template")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_STORE + ")")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] bytes = inventoryImportService.buildTemplateXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_import_ton_kho.xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(bytes);
    }
}
