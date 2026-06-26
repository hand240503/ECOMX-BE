package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.services.promotion.PromotionImportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * Import các chương trình giá/khuyến mãi bằng Excel/CSV/TXT:
 * PC (đổi giá), PWP (mua kèm), Mix &amp; Match (giá theo số lượng).
 */
@RestController
@RequestMapping("${api.prefix}/admin/promotions")
public class AdminPromotionImportController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PromotionImportService promotionImportService;

    public AdminPromotionImportController(PromotionImportService promotionImportService) {
        this.promotionImportService = promotionImportService;
    }

    // ───────────────────────── Price Change (PC) ─────────────────────────────

    @PostMapping(value = "/price-changes/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> importPriceChanges(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "startAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startAt,
            @RequestParam(value = "endAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endAt) {
        return ok(promotionImportService.importPriceChanges(file, startAt, endAt));
    }

    @GetMapping("/price-changes/import/template")
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<byte[]> priceChangeTemplate() {
        return template(promotionImportService.buildPriceChangeTemplate(), "mau_import_doi_gia.xlsx");
    }

    // ──────────────────── Purchase With Purchase (PWP) ───────────────────────

    @PostMapping(value = "/purchase-with-purchase/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> importPwp(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "startAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startAt,
            @RequestParam(value = "endAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endAt) {
        return ok(promotionImportService.importPurchaseWithPurchase(file, startAt, endAt));
    }

    @GetMapping("/purchase-with-purchase/import/template")
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<byte[]> pwpTemplate() {
        return template(promotionImportService.buildPurchaseWithPurchaseTemplate(), "mau_import_mua_kem.xlsx");
    }

    // ──────────────────── Mix & Match (Volume tier) ──────────────────────────

    @PostMapping(value = "/volume-price-tiers/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> importVolumeTiers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "startAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startAt,
            @RequestParam(value = "endAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endAt) {
        return ok(promotionImportService.importVolumeTiers(file, startAt, endAt));
    }

    @GetMapping("/volume-price-tiers/import/template")
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<byte[]> volumeTierTemplate() {
        return template(promotionImportService.buildVolumeTierTemplate(), "mau_import_mix_match.xlsx");
    }

    // ──────────────────────────── helpers ────────────────────────────────────

    private ResponseEntity<APIResponse<CatalogImportResponse>> ok(CatalogImportResponse result) {
        String msg = String.format("Thành công %d, lỗi %d",
                result.getCreatedCount() + result.getUpdatedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    private ResponseEntity<byte[]> template(byte[] bytes, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(bytes);
    }
}
