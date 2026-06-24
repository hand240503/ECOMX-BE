package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.GetProductsByIdsRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.PaginationMetadata;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductImportResponse;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.services.product.ProductExportService;
import com.ndh.ShopTechnology.services.product.ProductImportService;
import com.ndh.ShopTechnology.services.product.ProductService;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${api.prefix}/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final ProductImportService productImportService;
    private final ProductExportService productExportService;
    private final com.ndh.ShopTechnology.services.product.ProductFlagImportService productFlagImportService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public AdminProductController(ProductService productService,
                                  ProductImportService productImportService,
                                  ProductExportService productExportService,
                                  com.ndh.ShopTechnology.services.product.ProductFlagImportService productFlagImportService,
                                  com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.productService = productService;
        this.productImportService = productImportService;
        this.productExportService = productExportService;
        this.productFlagImportService = productFlagImportService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/by-ids")
    @PreAuthorize("@perm.check(100002)")
    public ResponseEntity<APIResponse<List<ProductFullResponse>>> getProductsByIds(
            @Valid @RequestBody GetProductsByIdsRequest request) {
        List<ProductFullResponse> products = productService.getProductsByIds(request.getProductIds());
        APIResponse<List<ProductFullResponse>> response = APIResponse.of(
                true,
                "Products retrieved successfully",
                products,
                null,
                null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-ids")
    @PreAuthorize("@perm.check(100002)")
    public ResponseEntity<APIResponse<List<ProductFullResponse>>> getProductsByIdsFromQuery(
            @RequestParam(value = "ids", required = false) String idsParam) {
        if (idsParam == null || idsParam.isBlank()) {
            APIResponse<List<ProductFullResponse>> response = APIResponse.of(
                    false,
                    "Query parameter ids is required (e.g. ?ids=1,2,3)",
                    null,
                    List.of(ErrorResponse.builder()
                            .field("ids")
                            .message("Comma-separated product ids required")
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        List<Long> productIds;
        try {
            productIds = Arrays.stream(idsParam.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            APIResponse<List<ProductFullResponse>> response = APIResponse.of(
                    false,
                    "Invalid ids parameter",
                    null,
                    List.of(ErrorResponse.builder()
                            .field("ids")
                            .message("Each id must be a number")
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (productIds.isEmpty()) {
            APIResponse<List<ProductFullResponse>> response = APIResponse.of(
                    false,
                    "No product ids parsed",
                    null,
                    List.of(ErrorResponse.builder().field("ids").message("empty").build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (productIds.size() > 200) {
            APIResponse<List<ProductFullResponse>> response = APIResponse.of(
                    false,
                    "At most 200 product ids per request",
                    null,
                    List.of(ErrorResponse.builder().field("ids").message("max 200").build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        List<ProductFullResponse> products = productService.getProductsByIds(productIds);
        APIResponse<List<ProductFullResponse>> response = APIResponse.of(
                true,
                "Products retrieved successfully",
                products,
                null,
                null);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@perm.check(100001)")
    public ResponseEntity<APIResponse<ProductFullResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        try {
            ProductFullResponse product = productService.createProduct(request);
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    true,
                    "Product created successfully",
                    product,
                    null,
                    null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    "Failed to create product: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("product")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Xem trước import (dry-run): KHÔNG ghi CSDL. Trả về từng sản phẩm kèm hành động mặc định
     * (CẬP NHẬT nếu đã tồn tại theo SKU/tên, ngược lại THÊM MỚI) + khóa để FE chỉnh chọn.
     */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(100001)")
    public ResponseEntity<APIResponse<ProductImportResponse>> previewImport(
            @RequestParam("file") MultipartFile file) {
        ProductImportResponse result = productImportService.previewProducts(file);
        String msg = String.format("Sẽ thêm mới %d, cập nhật %d, lỗi %d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    /**
     * Import sản phẩm hàng loạt từ file Excel (.xlsx) hoặc CSV/TXT.
     * Phần `file` là multipart; `actions` (tùy chọn) là JSON {"<key>":"CREATE|UPDATE"} chọn theo từng SP.
     * Mỗi sản phẩm lưu transaction riêng; dòng lỗi được báo cáo lại.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(100001)")
    public ResponseEntity<APIResponse<ProductImportResponse>> importProducts(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "actions", required = false) String actionsJson) {
        java.util.Map<String, String> actions = null;
        if (actionsJson != null && !actionsJson.isBlank()) {
            try {
                actions = objectMapper.readValue(actionsJson,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {});
            } catch (Exception e) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "Tham số 'actions' không hợp lệ (JSON)");
            }
        }
        ProductImportResponse result = productImportService.importProducts(file, actions);
        String msg = String.format("Đã thêm mới %d, cập nhật %d, lỗi %d (%d biến thể)",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFailureCount(),
                result.getCreatedVariantCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    /** Tải file Excel mẫu để điền dữ liệu import. */
    @GetMapping(value = "/import/template")
    @PreAuthorize("@perm.check(100001)")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] bytes = productImportService.buildTemplateXlsx();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"mau_import_san_pham.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ── Đánh dấu Nổi bật / Hot-sale bằng Excel ───────────────────────────────

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Import đánh dấu sản phẩm NỔI BẬT từ file (multipart 'file'). */
    @PostMapping(value = "/featured/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(100001)")
    public ResponseEntity<APIResponse<com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse>> importFeatured(
            @RequestParam("file") MultipartFile file) {
        var result = productFlagImportService.importFeatured(file);
        String msg = String.format("Đã đánh dấu nổi bật %d, bỏ qua %d, lỗi %d",
                result.getCreatedCount(), result.getSkippedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    /** Import đánh dấu sản phẩm HOT-SALE từ file (multipart 'file'). */
    @PostMapping(value = "/hot-sale/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(100001)")
    public ResponseEntity<APIResponse<com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse>> importHotSale(
            @RequestParam("file") MultipartFile file) {
        var result = productFlagImportService.importHotSale(file);
        String msg = String.format("Đã đánh dấu hot-sale %d, bỏ qua %d, lỗi %d",
                result.getCreatedCount(), result.getSkippedCount(), result.getFailureCount());
        return ResponseEntity.ok(APIResponse.of(true, msg, result, null, null));
    }

    /** File Excel mẫu cho import nổi bật / hot-sale (cột sku + product_id). */
    @GetMapping(value = {"/featured/import/template", "/hot-sale/import/template"})
    @PreAuthorize("@perm.check(100001)")
    public ResponseEntity<byte[]> downloadFlagTemplate() {
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"mau_import_noi_bat_hot_sale.xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(productFlagImportService.buildTemplateXlsx());
    }

    /**
     * Xuất toàn bộ sản phẩm (danh mục, thương hiệu, biến thể, giá, đơn vị) ra Excel,
     * tiêu đề theo tên cột CSDL.
     */
    @GetMapping(value = "/export")
    @PreAuthorize("@perm.check(100002)")
    public ResponseEntity<byte[]> exportProducts() {
        byte[] bytes = productExportService.exportProductsXlsx();
        String fileName = "san_pham_export_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".xlsx";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Xuất các sản phẩm CHƯA HOÀN THIỆN ra Excel: chưa có biến thể, hoặc có biến thể nhưng
     * chưa có giá (cột giá/đơn vị để trống để rà soát & bổ sung). Quyền 100002.
     */
    @GetMapping(value = "/export/incomplete")
    @PreAuthorize("@perm.check(100002)")
    public ResponseEntity<byte[]> exportIncompleteProducts() {
        byte[] bytes = productExportService.exportIncompleteProductsXlsx();
        String fileName = "san_pham_chua_hoan_thien_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".xlsx";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping
    @PreAuthorize("@perm.check(100002)")
    public ResponseEntity<APIResponse<List<ProductFullResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Page<ProductFullResponse> productPage = productService.getAdminProducts(page, limit);
        List<ProductFullResponse> products = productPage.getContent();
        PaginationMetadata metadata = PaginationMetadata.fromPage(productPage);

        APIResponse<List<ProductFullResponse>> response = APIResponse.<List<ProductFullResponse>>builder()
                .success(true)
                .message("Products retrieved successfully")
                .data(products)
                .metadata(metadata)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("@perm.check(100002)")
    public ResponseEntity<APIResponse<List<ProductFullResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            Page<ProductFullResponse> productPage =
                    productService.getAdminProductsByCategoryId(categoryId, page, limit);
            List<ProductFullResponse> products = productPage.getContent();
            PaginationMetadata metadata = PaginationMetadata.fromPage(productPage);

            APIResponse<List<ProductFullResponse>> response = APIResponse.<List<ProductFullResponse>>builder()
                    .success(true)
                    .message("Products retrieved successfully")
                    .data(products)
                    .metadata(metadata)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<ProductFullResponse>> response = APIResponse.of(
                    false,
                    "Failed to get products: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("categoryId")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(100002)")
    public ResponseEntity<APIResponse<ProductFullResponse>> getProductById(@PathVariable Long id) {
        try {
            ProductFullResponse product = productService.getProductById(id);
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    true,
                    "Product retrieved successfully",
                    product,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    "Product not found",
                    null,
                    List.of(ErrorResponse.builder()
                            .field("id")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@perm.check(100003)")
    public ResponseEntity<APIResponse<ProductFullResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        try {
            ProductFullResponse product = productService.updateProduct(id, request, null);
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    true,
                    "Product updated successfully",
                    product,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (NotFoundEntityException e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("product")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (CustomApiException e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("product")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(e.getStatus()).body(response);
        } catch (Exception e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    "Failed to update product: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("product")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(100003)")
    public ResponseEntity<APIResponse<ProductFullResponse>> updateProductWithImages(
            @PathVariable Long id,
            @RequestPart("product") @Valid UpdateProductRequest request,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) {
        try {
            ProductFullResponse product = productService.updateProduct(id, request, newImages);
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    true,
                    "Product updated successfully",
                    product,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (NotFoundEntityException e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("product")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (CustomApiException e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("product")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(e.getStatus()).body(response);
        } catch (Exception e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    "Failed to update product: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("product")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping(value = "/{productId}/variants/{variantId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(100003)")
    public ResponseEntity<APIResponse<ProductFullResponse>> addVariantImages(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "mainNewImageIndex", required = false) Integer mainNewImageIndex) {
        try {
            ProductFullResponse product =
                    productService.addVariantImages(productId, variantId, newImages, mainNewImageIndex);
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    true,
                    "Variant images uploaded successfully",
                    product,
                    null,
                    null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NotFoundEntityException e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("variant")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (CustomApiException e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("images")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(e.getStatus()).body(response);
        } catch (Exception e) {
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    false,
                    "Failed to upload variant images: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("images")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(100004)")
    public ResponseEntity<APIResponse<Void>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            APIResponse<Void> response = APIResponse.of(
                    true,
                    "Product deleted successfully",
                    null,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<Void> response = APIResponse.of(
                    false,
                    "Failed to delete product: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("id")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
