  package com.ndh.ShopTechnology.controller.product;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.PaginationMetadata;
import com.ndh.ShopTechnology.dto.response.ProductSearchPaginationMetadata;
import com.ndh.ShopTechnology.dto.response.product.ProductDetailResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.search.ProductSearchResult;
import com.ndh.ShopTechnology.services.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/products")
public class ProductController {

  private final ProductService productService;

  @Autowired
  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping
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
  public ResponseEntity<APIResponse<List<ProductFullResponse>>> getAllProducts(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int limit) {
    Page<ProductFullResponse> productPage = productService.getProducts(page, limit);

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

  @GetMapping("/featured")
  public ResponseEntity<APIResponse<List<ProductFullResponse>>> getFeaturedProducts(
      @RequestParam(defaultValue = "10") int limit) {
    List<ProductFullResponse> products = productService.getFeaturedProducts(limit);
    APIResponse<List<ProductFullResponse>> response = APIResponse.of(
        true,
        "Featured products retrieved successfully",
        products,
        null,
        null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/best-sellers")
  public ResponseEntity<APIResponse<List<ProductFullResponse>>> getBestSellingProducts(
      @RequestParam(defaultValue = "10") int limit) {
    List<ProductFullResponse> products = productService.getBestSellingProducts(limit);
    APIResponse<List<ProductFullResponse>> response = APIResponse.of(
        true,
        "Best selling products retrieved successfully",
        products,
        null,
        null);
    return ResponseEntity.ok(response);
  }

  /**
   * Full-text style search on product name, description, and tag (active items). Pagination metadata may
   * include {@code suggestedQuery} / {@code spellSuggestion} when the backend provides an alternate keyword.
   */
  @GetMapping("/search")
  public ResponseEntity<APIResponse<List<ProductFullResponse>>> searchProducts(
      @RequestParam(value = "q", required = false, defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int limit) {
    ProductSearchResult result = productService.searchProducts(q, page, limit);
    List<ProductFullResponse> products = result.page().getContent();
    ProductSearchPaginationMetadata metadata =
        ProductSearchPaginationMetadata.fromPage(result.page(), result.suggestedQuery());

    APIResponse<List<ProductFullResponse>> response = APIResponse.<List<ProductFullResponse>>builder()
        .success(true)
        .message("Products search completed")
        .data(products)
        .metadata(metadata)
        .build();

    return ResponseEntity.ok(response);
  }

  /**
   * PDP: full {@link ProductFullResponse} plus {@code recommendations} (similar products), same hybrid
   * engine as {@code GET .../recommendations/pdp/{productId}}.
   */
  @GetMapping("/{id}/detail")
  public ResponseEntity<APIResponse<ProductDetailResponse>> getProductDetail(
      @PathVariable Long id,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) String sessionId,
      @RequestParam(defaultValue = "10") int recommendationLimit) {
    try {
      ProductDetailResponse detail = productService.getProductDetail(id, userId, sessionId, recommendationLimit);
      APIResponse<ProductDetailResponse> response = APIResponse.of(
          true,
          "Product detail retrieved successfully",
          detail,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<ProductDetailResponse> response = APIResponse.of(
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

  @GetMapping("/{id}")
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

  @GetMapping("/category/{categoryId}")
  public ResponseEntity<APIResponse<List<ProductFullResponse>>> getProductsByCategory(
      @PathVariable Long categoryId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int limit) {
    try {
      Page<ProductFullResponse> productPage =
          productService.getProductsByCategoryId(categoryId, page, limit);
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

  @PutMapping("/{id}")
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
