package com.ndh.ShopTechnology.controller.product;

import com.ndh.ShopTechnology.dto.request.product.CreateProductRequest;
import com.ndh.ShopTechnology.dto.request.product.UpdateProductRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductResponse;
import com.ndh.ShopTechnology.services.product.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Operation(summary = "Create product", description = "Create a new product")
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

  @Operation(summary = "Get all products", description = "Get all products")
  @GetMapping
  public ResponseEntity<APIResponse<List<ProductResponse>>> getAllProducts() {
    List<ProductResponse> products = productService.getAllProducts();
    APIResponse<List<ProductResponse>> response = APIResponse.of(
        true,
        "Products retrieved successfully",
        products,
        null,
        null);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get product by ID", description = "Get a specific product by ID")
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

  @Operation(summary = "Get products by category", description = "Get all products in a specific category")
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

  @Operation(summary = "Update product", description = "Update an existing product")
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

  @Operation(summary = "Delete product", description = "Delete a product by ID")
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
