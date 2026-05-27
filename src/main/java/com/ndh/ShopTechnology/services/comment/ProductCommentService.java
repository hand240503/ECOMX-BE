package com.ndh.ShopTechnology.services.comment;

import com.ndh.ShopTechnology.dto.request.comment.AdminUpdateProductCommentRequest;
import com.ndh.ShopTechnology.dto.request.comment.CreateProductCommentRequest;
import com.ndh.ShopTechnology.dto.request.comment.UpdateProductCommentRequest;
import com.ndh.ShopTechnology.dto.response.comment.ProductCommentResponse;

import java.util.List;

public interface ProductCommentService {

    ProductCommentResponse createComment(CreateProductCommentRequest request);

    ProductCommentResponse updateMyComment(Long commentId, UpdateProductCommentRequest request);

    void deleteMyComment(Long commentId);

    List<ProductCommentResponse> getVisibleCommentsByProduct(Long productId);

    List<ProductCommentResponse> getAllCommentsByProduct(Long productId);

    List<ProductCommentResponse> getAllComments();

    ProductCommentResponse getCommentById(Long commentId);

    ProductCommentResponse adminUpdateComment(Long commentId, AdminUpdateProductCommentRequest request);

    void adminDeleteComment(Long commentId);
}
