package com.ndh.ShopTechnology.dto.response.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.comment.ProductCommentEntity;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCommentResponse {

    private Long id;
    private Long userId;
    private String username;
    private String userFullName;
    private Long productId;
    private String productName;
    private String content;
    private Boolean isHidden;
    private Date createdDate;
    private Date modifiedDate;

    public static ProductCommentResponse fromEntity(ProductCommentEntity entity) {
        if (entity == null) return null;

        ProductCommentResponseBuilder builder = ProductCommentResponse.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .isHidden(entity.getIsHidden())
                .createdDate(entity.getCreatedDate())
                .modifiedDate(entity.getModifiedDate());

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                   .username(entity.getUser().getUsername());
            if (entity.getUser().getUserInfo() != null) {
                builder.userFullName(entity.getUser().getUserInfo().getFullName());
            }
        }

        if (entity.getProduct() != null) {
            builder.productId(entity.getProduct().getId())
                   .productName(entity.getProduct().getProductName());
        }

        return builder.build();
    }
}
