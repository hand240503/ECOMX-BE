package com.ndh.ShopTechnology.dto.response.unit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
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
public class UnitResponse {

    private Long id;

    private String code;

    @JsonProperty("name_unit")
    private String nameUnit;

    private Integer ratio;
    private Integer status;
    private Date createdDate;
    private Date modifiedDate;

    public static UnitResponse fromEntity(UnitEntity e) {
        if (e == null) {
            return null;
        }
        return UnitResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .nameUnit(e.getNameUnit())
                .ratio(e.getRatio())
                .status(e.getStatus())
                .createdDate(e.getCreatedDate())
                .modifiedDate(e.getModifiedDate())
                .build();
    }
}
