package com.ndh.ShopTechnology.entities.comment;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Bình luận (comment) của khách hàng đã mua sản phẩm.
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Chỉ user đã có đơn hàng COMPLETED chứa sản phẩm mới được tạo comment.</li>
 *   <li>Mỗi user chỉ được có 1 comment trên mỗi sản phẩm ({@code UNIQUE(user_id, product_id)}).</li>
 *   <li>User chỉ có thể sửa/xoá comment của chính mình.</li>
 *   <li>Người có quyền quản lý sản phẩm ({@code READ/UPDATE/DELETE_PRODUCT}) được CRUD tất cả comment.</li>
 * </ul>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "product_comments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
public class ProductCommentEntity extends BaseEntity {

    public static final String COL_USER_ID    = "user_id";
    public static final String COL_PRODUCT_ID = "product_id";
    public static final String COL_CONTENT    = "content";
    public static final String COL_IS_HIDDEN  = "is_hidden";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_USER_ID, nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PRODUCT_ID, nullable = false)
    private ProductEntity product;

    /** Nội dung bình luận. */
    @Column(name = COL_CONTENT, columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Admin ẩn comment (soft-hide) mà không xoá hẳn.
     * Comment bị ẩn không hiển thị ở phía khách hàng nhưng vẫn truy vấn được từ admin.
     */
    @Column(name = COL_IS_HIDDEN, nullable = false)
    @Builder.Default
    private Boolean isHidden = false;
}
