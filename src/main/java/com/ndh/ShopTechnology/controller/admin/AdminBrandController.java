package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.brand.CreateBrandRequest;
import com.ndh.ShopTechnology.dto.request.brand.UpdateBrandRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.brand.BrandResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.services.brand.BrandService;
import com.ndh.ShopTechnology.services.catalog.CatalogImportService;
import com.ndh.ShopTechnology.services.product.ProductExportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/brands")
public class AdminBrandController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final BrandService brandService;
    private final ProductExportService productExportService;
    private final CatalogImportService catalogImportService;

    public AdminBrandController(BrandService brandService, ProductExportService productExportService,
                               CatalogImportService catalogImportService) {
        this.brandService = brandService;
        this.productExportService = productExportService;
        this.catalogImportService = catalogImportService;
    }

    /** Xuất toàn bộ thương hiệu ra Excel (theo cột CSDL). */
    @GetMapping("/export")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<byte[]> export() {
        byte[] bytes = productExportService.exportBrandsXlsx();
        String fileName = "thuong_hieu_export_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(bytes);
    }

    /** Xem trước import (dry-run): KHÔNG ghi CSDL, chỉ trả về dòng nào sẽ thêm/cập nhật. */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> previewBrands(
            @RequestParam("file") MultipartFile file) {
        CatalogImportResponse result = catalogImportService.previewBrands(file);
        String msg = String.format("Sẽ thêm mới %d, cập nhật %d, lỗi %d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    /** Import/upsert thương hiệu từ file Excel/CSV/TXT (trùng -> ghi đè, chưa có -> thêm mới). */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> importBrands(
            @RequestParam("file") MultipartFile file) {
        CatalogImportResponse result = catalogImportService.importBrands(file);
        String msg = String.format("Thêm mới %d, cập nhật %d, lỗi %d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<BrandResponse>>> list() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", brandService.listAll(), null, null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<BrandResponse>> getById(@PathVariable long id) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", brandService.getById(id), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<BrandResponse>> create(@Valid @RequestBody CreateBrandRequest request) {
        BrandResponse data = brandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<BrandResponse>> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateBrandRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Updated", brandService.update(id, request), null, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRODUCT + ")")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable long id) {
        brandService.delete(id);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }
}
