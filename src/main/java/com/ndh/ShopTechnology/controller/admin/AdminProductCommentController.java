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

/**
 * API quản lý comment dành cho admin/staff có quyền quản lý sản phẩm.
 *
 * <p>Phân quyền theo permission code:
 * <ul>
 *   <li>{@code GET} — yêu cầu {@link PermissionCode#READ_PRODUCT} (100002) hoặc {@code READ_ALL} (102).</li>
 *   <li>{@code PUT} — yêu cầu {@link PermissionCode#UPDATE_PRODUCT} (100003) hoặc {@code UPDATE_ALL} (103).</li>
 *   <li>{@code DELETE} — yêu cầu {@link PermissionCode#DELETE_PRODUCT} (100004) hoặc {@code DELETE_ALL} (104).</li>
 * </ul>
 *
 * <p>Tất cả endpoint nằm dưới {@code /admin/**} nên đã được lọc role qua {@link com.ndh.ShopTechnology.config.WebSecurityConfig}
 * (chỉ SUPER_ADMIN / ADMIN / MANAGER / EMPLOYEE mới được vào). {@code @PreAuthorize} kiểm tra thêm permission code cụ thể.
 */
@RestController
@RequestMapping("${api.prefix}/admin/product-comments")
@RequiredArgsConstructor
public class AdminProductCommentController {

    private final ProductCommentService commentService;

    // ------------------------------------------------------------------
    // READ (READ_PRODUCT hoặc READ_ALL)
    // ------------------------------------------------------------------

    /**
     * Lấy toàn bộ comment trong hệ thống (kể cả bị ẩn).
     */
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

    /**
     * Lấy tất cả comment (kể cả bị ẩn) của một sản phẩm cụ thể.
     */
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

    /**
     * Lấy chi tiết một comment theo id.
     */
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

    // ------------------------------------------------------------------
    // UPDATE (UPDATE_PRODUCT hoặc UPDATE_ALL)
    // ------------------------------------------------------------------

    /**
     * Cập nhật nội dung hoặc trạng thái ẩn/hiện của comment.
     * Admin có thể sửa nội dung và ẩn comment vi phạm mà không cần xoá.
     */
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

    // ------------------------------------------------------------------
    // DELETE (DELETE_PRODUCT hoặc DELETE_ALL)
    // ------------------------------------------------------------------

    /**
     * Xoá hẳn comment (hard delete).
     * Dùng khi comment vi phạm nghiêm trọng; với vi phạm nhẹ nên dùng {@code isHidden=true} qua PUT.
     */
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
