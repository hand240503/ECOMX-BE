package com.ndh.ShopTechnology.dto.request.store;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Tất cả trường đều tùy chọn; chỉ trường khác null mới được cập nhật. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreUpdateRequest {

    @Size(max = 64, message = "Mã kho tối đa 64 ký tự")
    private String code;

    @Size(max = 255, message = "Tên kho tối đa 255 ký tự")
    private String name;

    @Size(max = 32)
    private String phone;

    private String addressLine;

    @Size(max = 128)
    private String city;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    private Boolean active;

    private Boolean isDefault;

    private String note;
}
