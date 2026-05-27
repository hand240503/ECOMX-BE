package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.comment.AdminUpdateProductCommentRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.comment.ProductCommentResponse;
import com.ndh.ShopTechnology.services.comment.ProductCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/admin/product-comments")
@RequiredArgsConstructor
public class AdminProductCommentController {

    private final ProductCommentService commentService;

    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<ProductCommentResponse>>> getAllComments() {
        List<ProductCommentResponse> comments = commentService.getAllComments();
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Lấy tất cả bình luận thành công",
                comments,
                null,
                Map.of("count", comments.size())));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<ProductCommentResponse>>> getCommentsByProduct(
            @PathVariable Long productId) {
        List<ProductCommentResponse> comments = commentService.getAllCommentsByProduct(productId);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Lấy danh sách bình luận theo sản phẩm thành công",
                comments,
                null,
                Map.of("count", comments.size(), "productId", productId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<ProductCommentResponse>> getCommentById(@PathVariable Long id) {
        ProductCommentResponse comment = commentService.getCommentById(id);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Lấy chi tiết bình luận thành công",
                comment,
                null,
                null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_PRODUCT + ")")
    public ResponseEntity<APIResponse<ProductCommentResponse>> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateProductCommentRequest request) {
        ProductCommentResponse comment = commentService.adminUpdateComment(id, request);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Cập nhật bình luận thành công",
                comment,
                null,
                null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.DELETE_PRODUCT + ")")
    public ResponseEntity<APIResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.adminDeleteComment(id);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Xoá bình luận thành công",
                null,
                null,
                null));
    }
}
