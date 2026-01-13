package com.ndh.ShopTechnology.dto.response.rating;

import com.ndh.ShopTechnology.entities.rating.UserRatingEntity;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRatingResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long productId;
    private String productName;
    private Integer rating;
    private String comment;
    private Date createdDate;
    private Date modifiedDate;

    public static UserRatingResponse fromEntity(UserRatingEntity entity) {
        if (entity == null)
            return null;

        UserRatingResponseBuilder builder = UserRatingResponse.builder()
                .id(entity.getId())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .createdDate(entity.getCreatedDate())
                .modifiedDate(entity.getModifiedDate());

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .username(entity.getUser().getUsername());
        }

        if (entity.getProduct() != null) {
            builder.productId(entity.getProduct().getId())
                    .productName(entity.getProduct().getProductName());
        }

        return builder.build();
    }
}
