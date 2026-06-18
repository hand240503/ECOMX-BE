package com.ndh.ShopTechnology.dto.response.rating;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String fullName;
    private String avatar;
    private Long productId;
    private String productName;

    @JsonProperty("l_description")
    private String lDescription;

    /** Thang sao 1–5 (explicit). */
    private Double rating;
    /** 0/null = explicit (người dùng chấm), 1 = implicit (builder sinh). */
    private Integer type;
    private String comment;
    private Date createdDate;
    private Date modifiedDate;

    public static UserRatingResponse fromEntity(UserRatingEntity entity) {
        if (entity == null)
            return null;

        UserRatingResponseBuilder builder = UserRatingResponse.builder()
                .id(entity.getId())
                .rating(entity.getRating())
                .type(entity.getType())
                .comment(entity.getComment())
                .createdDate(entity.getCreatedDate())
                .modifiedDate(entity.getModifiedDate());

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .username(entity.getUser().getUsername());
            var info = entity.getUser().getUserInfo();
            if (info != null) {
                builder.fullName(info.getFullName())
                        .avatar(info.getAvatar());
            }
        }

        if (entity.getProduct() != null) {
            builder.productId(entity.getProduct().getId())
                    .productName(entity.getProduct().getProductName())
                    .lDescription(entity.getProduct().getLDescription());
        }

        return builder.build();
    }
}
