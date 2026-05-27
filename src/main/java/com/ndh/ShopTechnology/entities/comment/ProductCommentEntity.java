package com.ndh.ShopTechnology.entities.comment;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = COL_CONTENT, columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = COL_IS_HIDDEN, nullable = false)
    @Builder.Default
    private Boolean isHidden = false;
}
