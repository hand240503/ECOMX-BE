package com.ndh.ShopTechnology.dto.response.brand;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.product.BrandEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandResponse {

    private Long id;
    private String code;
    private String name;
    private Integer status;
    private Date createdDate;
    private Date modifiedDate;

    public static BrandResponse fromEntity(BrandEntity e) {
        if (e == null) {
            return null;
        }
        return BrandResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .status(e.getStatus())
                .createdDate(e.getCreatedDate())
                .modifiedDate(e.getModifiedDate())
                .build();
    }
}
