package com.ndh.ShopTechnology.controller.comment;

import com.ndh.ShopTechnology.dto.request.comment.CreateProductCommentRequest;
import com.ndh.ShopTechnology.dto.request.comment.UpdateProductCommentRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.comment.ProductCommentResponse;
import com.ndh.ShopTechnology.services.comment.ProductCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API comment dành cho khách hàng (user đã đăng nhập + đã mua sản phẩm).
 *
 * <p>Phân quyền:
 * <ul>
 *   <li>{@code GET /product/{productId}} — public (không cần đăng nhập).</li>
 *   <li>{@code POST /} — cần đăng nhập + đã mua sản phẩm (kiểm tra trong service).</li>
 *   <li>{@code PUT /{id}} — cần đăng nhập + là chủ comment.</li>
 *   <li>{@code DELETE /{id}} — cần đăng nhập + là chủ comment.</li>
 * </ul>
 */
@RestController
@RequestMapping("${api.prefix}/product-comments")
@RequiredArgsConstructor
public class ProductCommentController {

    private final ProductCommentService commentService;

    // ------------------------------------------------------------------
    // PUBLIC
    // ------------------------------------------------------------------

    /**
     * Lấy tất cả comment hiển thị của một sản phẩm (public — không cần đăng nhập).
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<APIResponse<List<ProductCommentResponse>>> getVisibleComments(
            @PathVariable Long productId) {
        List<ProductCommentResponse> comments = commentService.getVisibleCommentsByProduct(productId);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Lấy danh sách bình luận thành công",
                comments,
                null,
                Map.of("count", comments.size(), "productId", productId)));
    }

    // ------------------------------------------------------------------
    // AUTHENTICATED USER
    // ------------------------------------------------------------------

    /**
     * Tạo bình luận mới.
     * Yêu cầu: đã đăng nhập + đã mua sản phẩm (đơn hàng COMPLETED).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<APIResponse<ProductCommentResponse>> createComment(
            @Valid @RequestBody CreateProductCommentRequest request) {
        ProductCommentResponse comment = commentService.createComment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.of(
                true,
                "Bình luận đã được tạo thành công",
                comment,
                null,
                null));
    }

    /**
     * Cập nhật bình luận của chính mình.
     * Yêu cầu: đã đăng nhập + là chủ comment.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<APIResponse<ProductCommentResponse>> updateMyComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductCommentRequest request) {
        ProductCommentResponse comment = commentService.updateMyComment(id, request);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Bình luận đã được cập nhật thành công",
                comment,
                null,
                null));
    }

    /**
     * Xoá bình luận của chính mình.
     * Yêu cầu: đã đăng nhập + là chủ comment.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<APIResponse<Void>> deleteMyComment(@PathVariable Long id) {
        commentService.deleteMyComment(id);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Bình luận đã được xoá thành công",
                null,
                null,
                null));
    }
}
