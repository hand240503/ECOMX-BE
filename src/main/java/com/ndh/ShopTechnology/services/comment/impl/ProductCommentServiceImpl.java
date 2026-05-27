package com.ndh.ShopTechnology.services.comment.impl;

import com.ndh.ShopTechnology.constants.OrderConstants;
import com.ndh.ShopTechnology.dto.request.comment.AdminUpdateProductCommentRequest;
import com.ndh.ShopTechnology.dto.request.comment.CreateProductCommentRequest;
import com.ndh.ShopTechnology.dto.request.comment.UpdateProductCommentRequest;
import com.ndh.ShopTechnology.dto.response.comment.ProductCommentResponse;
import com.ndh.ShopTechnology.entities.comment.ProductCommentEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.OrderDetailRepository;
import com.ndh.ShopTechnology.repository.ProductCommentRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.comment.ProductCommentService;
import com.ndh.ShopTechnology.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommentServiceImpl implements ProductCommentService {

    private final ProductCommentRepository commentRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserService userService;

    @Override
    @Transactional
    public ProductCommentResponse createComment(CreateProductCommentRequest request) {
        UserEntity currentUser = userService.getCurrentUser();

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundEntityException(
                        "Sản phẩm không tồn tại: id=" + request.getProductId()));

        boolean hasPurchased = orderDetailRepository.existsByOrderUserIdAndProductIdAndOrderStatus(
                currentUser.getId(),
                request.getProductId(),
                OrderConstants.STATUS_COMPLETED);

        if (!hasPurchased) {
            throw new CustomApiException(
                    HttpStatus.FORBIDDEN,
                    "Bạn chỉ có thể bình luận sản phẩm mà bạn đã mua thành công.");
        }

        if (commentRepository.existsByUser_IdAndProduct_Id(currentUser.getId(), product.getId())) {
            throw new CustomApiException(
                    HttpStatus.CONFLICT,
                    "Bạn đã bình luận sản phẩm này rồi. Vui lòng chỉnh sửa bình luận hiện có.");
        }

        ProductCommentEntity comment = ProductCommentEntity.builder()
                .user(currentUser)
                .product(product)
                .content(request.getContent().trim())
                .isHidden(false)
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment created: userId={}, productId={}, commentId={}",
                currentUser.getId(), product.getId(), comment.getId());
        return ProductCommentResponse.fromEntity(comment);
    }

    @Override
    @Transactional
    public ProductCommentResponse updateMyComment(Long commentId, UpdateProductCommentRequest request) {
        UserEntity currentUser = userService.getCurrentUser();
        ProductCommentEntity comment = findCommentOrThrow(commentId);

        assertCommentOwner(comment, currentUser);

        if (StringUtils.hasText(request.getContent())) {
            comment.setContent(request.getContent().trim());
        }

        comment = commentRepository.save(comment);
        log.info("Comment updated by owner: commentId={}, userId={}", commentId, currentUser.getId());
        return ProductCommentResponse.fromEntity(comment);
    }

    @Override
    @Transactional
    public void deleteMyComment(Long commentId) {
        UserEntity currentUser = userService.getCurrentUser();
        ProductCommentEntity comment = findCommentOrThrow(commentId);

        assertCommentOwner(comment, currentUser);

        commentRepository.delete(comment);
        log.info("Comment deleted by owner: commentId={}, userId={}", commentId, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCommentResponse> getVisibleCommentsByProduct(Long productId) {
        assertProductExists(productId);
        return commentRepository
                .findByProduct_IdAndIsHiddenFalseOrderByCreatedDateDesc(productId)
                .stream()
                .map(ProductCommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCommentResponse> getAllCommentsByProduct(Long productId) {
        assertProductExists(productId);
        return commentRepository
                .findByProduct_IdOrderByCreatedDateDesc(productId)
                .stream()
                .map(ProductCommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCommentResponse> getAllComments() {
        return commentRepository.findAllOrderByCreatedDateDesc()
                .stream()
                .map(ProductCommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCommentResponse getCommentById(Long commentId) {
        return ProductCommentResponse.fromEntity(findCommentOrThrow(commentId));
    }

    @Override
    @Transactional
    public ProductCommentResponse adminUpdateComment(Long commentId, AdminUpdateProductCommentRequest request) {
        ProductCommentEntity comment = findCommentOrThrow(commentId);

        if (StringUtils.hasText(request.getContent())) {
            comment.setContent(request.getContent().trim());
        }
        if (request.getIsHidden() != null) {
            comment.setIsHidden(request.getIsHidden());
        }

        comment = commentRepository.save(comment);
        log.info("Comment updated by admin: commentId={}", commentId);
        return ProductCommentResponse.fromEntity(comment);
    }

    @Override
    @Transactional
    public void adminDeleteComment(Long commentId) {
        ProductCommentEntity comment = findCommentOrThrow(commentId);
        commentRepository.delete(comment);
        log.info("Comment deleted by admin: commentId={}", commentId);
    }

    private ProductCommentEntity findCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundEntityException("Comment không tồn tại: id=" + commentId));
    }

    private void assertCommentOwner(ProductCommentEntity comment, UserEntity currentUser) {
        if (comment.getUser() == null || !comment.getUser().getId().equals(currentUser.getId())) {
            throw new CustomApiException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền thực hiện thao tác này trên comment của người khác.");
        }
    }

    private void assertProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundEntityException("Sản phẩm không tồn tại: id=" + productId);
        }
    }
}
