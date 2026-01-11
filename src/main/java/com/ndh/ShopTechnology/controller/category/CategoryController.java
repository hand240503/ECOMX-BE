package com.ndh.ShopTechnology.controller.category;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.dto.request.category.CreateCategoryRequest;
import com.ndh.ShopTechnology.dto.request.category.UpdateCategoryRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.category.CategoryResponse;
import com.ndh.ShopTechnology.services.category.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/categories")
public class CategoryController {

  private final CategoryService categoryService;

  @Autowired
  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @Operation(summary = "Create category", description = "Create a new category (parent or child)")
  @PostMapping
  public ResponseEntity<APIResponse<CategoryResponse>> createCategory(
      @Valid @RequestBody CreateCategoryRequest req) {
    try {
      CategoryResponse category = categoryService.createCategory(req);
      APIResponse<CategoryResponse> response = APIResponse.of(
          true,
          MessageConstant.CATEGORY_CREATE_SUCCESS,
          category,
          null,
          null);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      APIResponse<CategoryResponse> response = APIResponse.of(
          false,
          "Failed to create category: " + e.getMessage(),
          null,
          List.of(ErrorResponse.builder()
              .field("category")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }

  @Operation(summary = "Get all categories", description = "Get all categories")
  @GetMapping
  public ResponseEntity<APIResponse<List<CategoryResponse>>> getAllCategories() {
    List<CategoryResponse> categories = categoryService.getAllCategories();
    APIResponse<List<CategoryResponse>> response = APIResponse.of(
        true,
        MessageConstant.CATEGORY_LIST_SUCCESS,
        categories,
        null,
        null);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get root categories", description = "Get only parent categories (no parent)")
  @GetMapping("/roots")
  public ResponseEntity<APIResponse<List<CategoryResponse>>> getRootCategories() {
    List<CategoryResponse> categories = categoryService.getRootCategories();
    APIResponse<List<CategoryResponse>> response = APIResponse.of(
        true,
        "Root categories retrieved successfully",
        categories,
        null,
        null);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get child categories", description = "Get all child categories of a parent")
  @GetMapping("/parent/{parentId}/children")
  public ResponseEntity<APIResponse<List<CategoryResponse>>> getChildCategories(@PathVariable Long parentId) {
    try {
      List<CategoryResponse> categories = categoryService.getChildCategories(parentId);
      APIResponse<List<CategoryResponse>> response = APIResponse.of(
          true,
          "Child categories retrieved successfully",
          categories,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<List<CategoryResponse>> response = APIResponse.of(
          false,
          "Failed to get child categories: " + e.getMessage(),
          null,
          List.of(ErrorResponse.builder()
              .field("parentId")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
  }

  @Operation(summary = "Get category by ID", description = "Get a specific category by ID")
  @GetMapping("/{id}")
  public ResponseEntity<APIResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
    try {
      CategoryResponse category = categoryService.getCategoryById(id);
      APIResponse<CategoryResponse> response = APIResponse.of(
          true,
          "Category retrieved successfully",
          category,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<CategoryResponse> response = APIResponse.of(
          false,
          "Category not found",
          null,
          List.of(ErrorResponse.builder()
              .field("id")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
  }

  @Operation(summary = "Update category", description = "Update an existing category")
  @PutMapping("/{id}")
  public ResponseEntity<APIResponse<CategoryResponse>> updateCategory(
      @PathVariable Long id,
      @Valid @RequestBody UpdateCategoryRequest req) {
    try {
      CategoryResponse category = categoryService.updateCategory(id, req);
      APIResponse<CategoryResponse> response = APIResponse.of(
          true,
          "Category updated successfully",
          category,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<CategoryResponse> response = APIResponse.of(
          false,
          "Failed to update category: " + e.getMessage(),
          null,
          List.of(ErrorResponse.builder()
              .field("category")
              .message(e.getMessage())
              .build()),
          null);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }

  @Operation(summary = "Delete category", description = "Delete a category by ID")
  @DeleteMapping("/{id}")
  public ResponseEntity<APIResponse<Void>> deleteCategory(@PathVariable Long id) {
    try {
      categoryService.deleteCategory(id);
      APIResponse<Void> response = APIResponse.of(
          true,
          "Category deleted successfully",
          null,
          null,
          null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      APIResponse<Void> response = APIResponse.of(
          false,
          "Failed to delete category: " + e.getMessage(),
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