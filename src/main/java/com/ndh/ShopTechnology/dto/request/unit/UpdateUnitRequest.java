package com.ndh.ShopTechnology.dto.request.unit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUnitRequest {

    @JsonProperty("name_unit")
    private String nameUnit;

    @Min(1)
    private Integer ratio;

    private Integer status;
}
