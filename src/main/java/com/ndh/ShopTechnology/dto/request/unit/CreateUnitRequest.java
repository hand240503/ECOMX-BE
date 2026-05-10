package com.ndh.ShopTechnology.dto.request.unit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateUnitRequest {

    @NotBlank(message = "name_unit is required")
    @JsonProperty("name_unit")
    private String nameUnit;

    /**
     * Hệ số quy đổi so với đơn vị cơ sở (vd 1 thùng = 12 cái). Null = 1.
     */
    @Min(1)
    private Integer ratio;

    /** Trạng thái (vd {@code SystemConstant.ACTIVE_STATUS}). Null = active. */
    private Integer status;
}
