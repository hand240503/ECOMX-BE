package com.ndh.ShopTechnology.entities.rating;

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
@Table(name = "user_ratings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
public class UserRatingEntity extends BaseEntity {

    public static final String COL_USER_ID = "user_id";
    public static final String COL_PRODUCT_ID = "product_id";
    public static final String COL_RATING = "rating";
    public static final String COL_COMMENT = "comment";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_USER_ID, nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PRODUCT_ID, nullable = false)
    private ProductEntity product;

    @Column(name = COL_RATING, nullable = false)
    private Integer rating; // Thường là 1-5

    @Column(name = COL_COMMENT, columnDefinition = "TEXT", nullable = true)
    private String comment;
}
