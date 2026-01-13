package com.ndh.ShopTechnology.controller.rating;

import com.ndh.ShopTechnology.dto.request.rating.CreateUserRatingRequest;
import com.ndh.ShopTechnology.dto.request.rating.UpdateUserRatingRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.rating.UserRatingResponse;
import com.ndh.ShopTechnology.services.rating.UserRatingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/user-ratings")
public class UserRatingController {

    private final UserRatingService userRatingService;

    @Autowired
    public UserRatingController(UserRatingService userRatingService) {
        this.userRatingService = userRatingService;
    }

    @Operation(summary = "Create user rating", description = "Create a new user rating for a product")
    @PostMapping
    public ResponseEntity<APIResponse<UserRatingResponse>> createRating(
            @Valid @RequestBody CreateUserRatingRequest request) {
        try {
            UserRatingResponse rating = userRatingService.createRating(request);
            APIResponse<UserRatingResponse> response = APIResponse.of(
                    true,
                    "User rating created successfully",
                    rating,
                    null,
                    null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            APIResponse<UserRatingResponse> response = APIResponse.of(
                    false,
                    "Failed to create user rating: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("rating")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @Operation(summary = "Get all user ratings", description = "Get all user ratings")
    @GetMapping
    public ResponseEntity<APIResponse<List<UserRatingResponse>>> getAllRatings() {
        try {
            List<UserRatingResponse> ratings = userRatingService.getAllRatings();
            APIResponse<List<UserRatingResponse>> response = APIResponse.of(
                    true,
                    "User ratings retrieved successfully",
                    ratings,
                    null,
                    Map.of("count", ratings.size()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<UserRatingResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve user ratings: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("ratings")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(summary = "Get user rating by ID", description = "Get a specific user rating by ID")
    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<UserRatingResponse>> getRatingById(@PathVariable Long id) {
        try {
            UserRatingResponse rating = userRatingService.getRatingById(id);
            APIResponse<UserRatingResponse> response = APIResponse.of(
                    true,
                    "User rating retrieved successfully",
                    rating,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<UserRatingResponse> response = APIResponse.of(
                    false,
                    "User rating not found",
                    null,
                    List.of(ErrorResponse.builder()
                            .field("id")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Get rating by user and product", description = "Get rating for a specific user and product")
    @GetMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<APIResponse<UserRatingResponse>> getRatingByUserAndProduct(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        try {
            UserRatingResponse rating = userRatingService.getRatingByUserAndProduct(userId, productId);
            APIResponse<UserRatingResponse> response = APIResponse.of(
                    true,
                    "User rating retrieved successfully",
                    rating,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<UserRatingResponse> response = APIResponse.of(
                    false,
                    "User rating not found",
                    null,
                    List.of(ErrorResponse.builder()
                            .field("userProduct")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Get ratings by user ID", description = "Get all ratings for a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<APIResponse<List<UserRatingResponse>>> getRatingsByUserId(@PathVariable Long userId) {
        try {
            List<UserRatingResponse> ratings = userRatingService.getRatingsByUserId(userId);
            APIResponse<List<UserRatingResponse>> response = APIResponse.of(
                    true,
                    "User ratings retrieved successfully",
                    ratings,
                    null,
                    Map.of("count", ratings.size(), "userId", userId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<UserRatingResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve user ratings: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("userId")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Get ratings by product ID", description = "Get all ratings for a specific product")
    @GetMapping("/product/{productId}")
    public ResponseEntity<APIResponse<List<UserRatingResponse>>> getRatingsByProductId(@PathVariable Long productId) {
        try {
            List<UserRatingResponse> ratings = userRatingService.getRatingsByProductId(productId);
            Double averageRating = userRatingService.getAverageRatingByProductId(productId);
            Long ratingCount = userRatingService.getRatingCountByProductId(productId);
            
            APIResponse<List<UserRatingResponse>> response = APIResponse.of(
                    true,
                    "User ratings retrieved successfully",
                    ratings,
                    null,
                    Map.of(
                            "count", ratings.size(),
                            "productId", productId,
                            "averageRating", averageRating,
                            "totalRatings", ratingCount));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<UserRatingResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve user ratings: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("productId")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Get average rating by product ID", description = "Get average rating for a specific product")
    @GetMapping("/product/{productId}/average")
    public ResponseEntity<APIResponse<Map<String, Object>>> getAverageRatingByProductId(@PathVariable Long productId) {
        try {
            Double averageRating = userRatingService.getAverageRatingByProductId(productId);
            Long ratingCount = userRatingService.getRatingCountByProductId(productId);
            
            APIResponse<Map<String, Object>> response = APIResponse.of(
                    true,
                    "Average rating retrieved successfully",
                    Map.of(
                            "productId", productId,
                            "averageRating", averageRating,
                            "totalRatings", ratingCount),
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<Map<String, Object>> response = APIResponse.of(
                    false,
                    "Failed to retrieve average rating: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("productId")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Update user rating", description = "Update an existing user rating")
    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<UserRatingResponse>> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRatingRequest request) {
        try {
            UserRatingResponse rating = userRatingService.updateRating(id, request);
            APIResponse<UserRatingResponse> response = APIResponse.of(
                    true,
                    "User rating updated successfully",
                    rating,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<UserRatingResponse> response = APIResponse.of(
                    false,
                    "Failed to update user rating: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("rating")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @Operation(summary = "Delete user rating", description = "Delete a user rating by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteRating(@PathVariable Long id) {
        try {
            userRatingService.deleteRating(id);
            APIResponse<Void> response = APIResponse.of(
                    true,
                    "User rating deleted successfully",
                    null,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<Void> response = APIResponse.of(
                    false,
                    "Failed to delete user rating: " + e.getMessage(),
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
