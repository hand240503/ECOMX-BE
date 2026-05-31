package com.ndh.ShopTechnology.dto.request.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateReturnStatusRequest {

    @NotNull(message = "Trạng thái trả hàng không được để trống")
    @Min(value = 1, message = "Trạng thái trả hàng phải từ 1 đến 5")
    @Max(value = 5, message = "Trạng thái trả hàng phải từ 1 đến 5")
    private Integer returnStatus;

    private String note;
}
