package com.ndh.ShopTechnology.dto.request.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái đơn hàng không được để trống")
    @Min(value = 1, message = "Trạng thái phải từ 1 đến 5")
    @Max(value = 5, message = "Trạng thái phải từ 1 đến 5")
    private Integer status;

    /** Lý do hủy — bắt buộc khi status = 5. Validate tại service layer. */
    @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự")
    private String cancelNote;
}
