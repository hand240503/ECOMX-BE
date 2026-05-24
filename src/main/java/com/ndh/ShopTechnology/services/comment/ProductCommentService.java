package com.ndh.ShopTechnology.services.comment;

import com.ndh.ShopTechnology.dto.request.comment.AdminUpdateProductCommentRequest;
import com.ndh.ShopTechnology.dto.request.comment.CreateProductCommentRequest;
import com.ndh.ShopTechnology.dto.request.comment.UpdateProductCommentRequest;
import com.ndh.ShopTechnology.dto.response.comment.ProductCommentResponse;

import java.util.List;

public interface ProductCommentService {

    // =====================================================================
    // USER-FACING — yêu cầu đã đăng nhập + đã mua sản phẩm
    // =====================================================================

    /**
     * Tạo comment mới. User hiện tại phải có đơn hàng COMPLETED chứa sản phẩm.
     * Mỗi user chỉ được comment 1 lần trên mỗi sản phẩm.
     */
    ProductCommentResponse createComment(CreateProductCommentRequest request);

    /**
     * User tự cập nhật comment của mình. Chỉ được sửa nội dung ({@code content}).
     * Sẽ lỗi nếu comment không thuộc user hiện tại.
     */
    ProductCommentResponse updateMyComment(Long commentId, UpdateProductCommentRequest request);

    /**
     * User tự xoá comment của mình.
     * Sẽ lỗi nếu comment không thuộc user hiện tại.
     */
    void deleteMyComment(Long commentId);

    /**
     * Lấy tất cả comment hiển thị (chưa bị ẩn) của một sản phẩm — public.
     */
    List<ProductCommentResponse> getVisibleCommentsByProduct(Long productId);

    // =====================================================================
    // ADMIN-FACING — yêu cầu quyền quản lý sản phẩm (READ/UPDATE/DELETE_PRODUCT)
    // =====================================================================

    /** Lấy tất cả comment (kể cả bị ẩn) của một sản phẩm. */
    List<ProductCommentResponse> getAllCommentsByProduct(Long productId);

    /** Lấy tất cả comment trong hệ thống. */
    List<ProductCommentResponse> getAllComments();

    /** Lấy chi tiết một comment theo id. */
    ProductCommentResponse getCommentById(Long commentId);

    /**
     * Admin cập nhật comment: có thể sửa nội dung và ẩn/hiện comment.
     */
    ProductCommentResponse adminUpdateComment(Long commentId, AdminUpdateProductCommentRequest request);

    /**
     * Admin xoá hẳn comment (hard delete).
     */
    void adminDeleteComment(Long commentId);
}
