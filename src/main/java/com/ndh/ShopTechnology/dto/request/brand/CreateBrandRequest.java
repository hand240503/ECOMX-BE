package com.ndh.ShopTechnology.dto.request.brand;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class CreateBrandRequest {

    /**
     * Mã hãng (duy nhất, so khớp không phân biệt hoa thường). Lưu dạng đã chuẩn hoá (trim + upper).
     */
    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "name is required")
    private String name;

    /** Trạng thái (vd {@code SystemConstant.ACTIVE_STATUS}). Null = active. */
    private Integer status;
}
