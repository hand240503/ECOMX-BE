package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.unit.CreateUnitRequest;
import com.ndh.ShopTechnology.dto.request.unit.UpdateUnitRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.unit.UnitResponse;
import com.ndh.ShopTechnology.services.unit.UnitImportService;
import com.ndh.ShopTechnology.services.unit.UnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/units")
public class AdminUnitController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final UnitService unitService;
    private final UnitImportService unitImportService;

    public AdminUnitController(UnitService unitService, UnitImportService unitImportService) {
        this.unitService = unitService;
        this.unitImportService = unitImportService;
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<UnitResponse>>> list() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", unitService.listAll(), null, null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<UnitResponse>> getById(@PathVariable long id) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", unitService.getById(id), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<UnitResponse>> create(@Valid @RequestBody CreateUnitRequest request) {
        UnitResponse data = unitService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<UnitResponse>> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateUnitRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Updated", unitService.update(id, request), null, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRODUCT + ")")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable long id) {
        unitService.delete(id);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }

    /** Import / upsert đơn vị tính từ file Excel/CSV/TXT (multipart 'file'). */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> importUnits(
            @RequestParam("file") MultipartFile file) {
        CatalogImportResponse result = unitImportService.importUnits(file);
        String msg = String.format("Thêm %d, cập nhật %d, bỏ qua %d, lỗi %d",
                result.getCreatedCount(), result.getUpdatedCount(),
                result.getSkippedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    /** Tải file Excel mẫu import đơn vị tính. */
    @GetMapping("/import/template")
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<byte[]> unitTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_import_don_vi_tinh.xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(unitImportService.buildTemplateXlsx());
    }
}
