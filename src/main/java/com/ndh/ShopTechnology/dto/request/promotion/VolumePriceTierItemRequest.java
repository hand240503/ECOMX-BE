package com.ndh.ShopTechnology.dto.request.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolumePriceTierItemRequest {

    @NotNull
    @Min(1)
    private Integer minQuantity;

    @NotNull
    @Min(0)
    private Double unitPrice;

    private Boolean enabled;
}
