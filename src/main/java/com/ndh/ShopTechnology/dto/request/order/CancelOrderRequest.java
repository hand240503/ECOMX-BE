package com.ndh.ShopTechnology.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {

    @NotBlank(message = "Lý do hủy đơn không được để trống")
    @Size(max = 500, message = "Lý do hủy đơn không được vượt quá 500 ký tự")
    private String reason;
}
