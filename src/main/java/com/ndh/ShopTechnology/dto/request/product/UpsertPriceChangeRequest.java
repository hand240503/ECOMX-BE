package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpsertPriceChangeRequest {

    @NotNull
    @Min(0)
    private Double basePrice;

    /** Nullable = không có giá ưu đãi. */
    @Min(0)
    private Double salePrice;

    @NotNull
    private Date startAt;

    private Date endAt;

    private Boolean enabled;
}

