package com.ndh.ShopTechnology.controller.product;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.PaginationMetadata;
import com.ndh.ShopTechnology.dto.response.product.ProductResponse;
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
  public ResponseEntity<APIResponse<ProductResponse>> createProduct(
      @Valid @RequestBody CreateProductRequest request) {
    try {
      ProductResponse product = productService.createProduct(request);
      APIResponse<ProductResponse> response = APIResponse.of(
          true,
          "Product created successfully",
          product,
          null,
          null);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      APIResponse<ProductResponse> response = APIResponse.of(
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
  public ResponseEntity<APIResponse<List<ProductResponse>>> getAllProducts(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int limit) {
    Page<ProductResponse> productPage = productService.getProducts(page, limit);

    List<ProductResponse> products = productPage.getContent();
    PaginationMetadata metadata = PaginationMetadata.fromPage(productPage);

    APIResponse<List<ProductResponse>> response = APIResponse.<List<ProductResponse>>builder()
        .success(true)
        .message("Products retrieved successfully")
        .data(products)
        .metadata(metadata)
        .build();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/featured")
  public ResponseEntity<APIResponse<List<ProductResponse>>> getFeaturedProducts(
      @RequestParam(defaultValue = "10") int limit) {
    List<ProductResponse> products = productService.getFeaturedProducts(limit);
    APIResponse<List<ProductResponse>> response = APIResponse.of(
        true,
        "Featured products retrieved successfully",
        products,
        null,
        null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/best-sellers")
  public ResponseEntity<APIResponse<List<ProductResponse>>> getBestSellingProducts(
      @RequestParam(defaultValue = "10") int limit) {
    List<ProductResponse> products = productService.getBestSellingProducts(limit);
    APIResponse<List<ProductResponse>> response = APIResponse.of(
        true,
        "Best selling products retrieved successfully",
        products,
        null,
        null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<APIResponse<ProductResponse>> getProductById(@PathVariable Long id) {
    try {
      ProductResponse product = productService.getProductById(id);
      APIResponse<ProductResponse> response = APIResponse.of(
          true,
          "Product retrieved successfully",
          product,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<ProductResponse> response = APIResponse.of(
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
  public ResponseEntity<APIResponse<List<ProductResponse>>> getProductsByCategory(@PathVariable Long categoryId) {
    try {
      List<ProductResponse> products = productService.getProductsByCategoryId(categoryId);
      APIResponse<List<ProductResponse>> response = APIResponse.of(
          true,
          "Products retrieved successfully",
          products,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<List<ProductResponse>> response = APIResponse.of(
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
  public ResponseEntity<APIResponse<ProductResponse>> updateProduct(
      @PathVariable Long id,
      @Valid @RequestBody UpdateProductRequest request) {
    try {
      ProductResponse product = productService.updateProduct(id, request);
      APIResponse<ProductResponse> response = APIResponse.of(
          true,
          "Product updated successfully",
          product,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<ProductResponse> response = APIResponse.of(
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
