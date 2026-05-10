package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.GetProductsByIdsRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.PaginationMetadata;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.services.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API quản trị sản phẩm — toàn bộ path dưới {@code {api.prefix}/admin/products}.
 * Yêu cầu JWT + role admin cụm (xem {@code WebSecurityConfig}); đọc/ghi kèm {@code @PreAuthorize} theo {@code PermissionCode}.
 */
@RestController
@RequestMapping("${api.prefix}/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
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

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(100003)")
    public ResponseEntity<APIResponse<ProductFullResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        try {
            ProductFullResponse product = productService.updateProduct(id, request);
            APIResponse<ProductFullResponse> response = APIResponse.of(
                    true,
                    "Product updated successfully",
                    product,
                    null,
                    null);
            return ResponseEntity.ok(response);
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
