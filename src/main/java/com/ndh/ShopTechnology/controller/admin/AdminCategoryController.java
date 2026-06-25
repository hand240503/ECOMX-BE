package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.category.BulkDeleteCategoryRequest;
import com.ndh.ShopTechnology.dto.request.category.CreateCategoryRequest;
import com.ndh.ShopTechnology.dto.request.category.UpdateCategoryRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.category.CategoryBulkDeleteResponse;
import com.ndh.ShopTechnology.dto.response.category.CategoryResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.services.category.CategoryService;
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
@RequestMapping("${api.prefix}/admin/categories")
public class AdminCategoryController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CategoryService categoryService;
    private final ProductExportService productExportService;
    private final CatalogImportService catalogImportService;

    public AdminCategoryController(CategoryService categoryService, ProductExportService productExportService,
                                  CatalogImportService catalogImportService) {
        this.categoryService = categoryService;
        this.productExportService = productExportService;
        this.catalogImportService = catalogImportService;
    }

    /** Xuất toàn bộ danh mục ra Excel (theo cột CSDL). */
    @GetMapping("/export")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<byte[]> export() {
        byte[] bytes = productExportService.exportCategoriesXlsx();
        String fileName = "danh_muc_export_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(bytes);
    }

    /** Xem trước import (dry-run): KHÔNG ghi CSDL, chỉ trả về dòng nào sẽ thêm/cập nhật. */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> previewCategories(
            @RequestParam("file") MultipartFile file) {
        CatalogImportResponse result = catalogImportService.previewCategories(file);
        String msg = String.format("Sẽ thêm mới %d, cập nhật %d, lỗi %d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    /** Import/upsert danh mục từ file Excel/CSV/TXT (trùng -> ghi đè, chưa có -> thêm mới). */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CatalogImportResponse>> importCategories(
            @RequestParam("file") MultipartFile file) {
        CatalogImportResponse result = catalogImportService.importCategories(file);
        String msg = String.format("Thêm mới %d, cập nhật %d, lỗi %d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<CategoryResponse>>> list() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", categoryService.getAllCategories(), null, null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<CategoryResponse>> getById(@PathVariable long id) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", categoryService.getCategoryById(id), null, null));
    }

    @PostMapping
    @PreAuthorize("@perm.check(" + PermissionCode.CREATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse data = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Created", data, null, null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CategoryResponse>> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Updated", categoryService.updateCategory(id, request), null, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRODUCT + ")")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(APIResponse.of(true, "Deleted", null, null, null));
    }

    /**
     * Xóa danh mục hàng loạt. Sản phẩm thuộc danh mục bị xóa sẽ được gỡ danh mục (set null),
     * danh mục con được đưa lên gốc (parent set null).
     */
    @PostMapping("/bulk-delete")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRODUCT + ")")
    public ResponseEntity<APIResponse<CategoryBulkDeleteResponse>> bulkDelete(
            @Valid @RequestBody BulkDeleteCategoryRequest request) {
        CategoryBulkDeleteResponse result = categoryService.deleteCategories(request.getIds());
        String msg = String.format("Đã xóa %d danh mục (gỡ %d sản phẩm, %d danh mục con lên gốc)",
                result.getDeleted(), result.getProductsDetached(), result.getChildrenDetached());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }
}
