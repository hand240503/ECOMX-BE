package com.ndh.ShopTechnology.dto.request.recommendation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SessionProfileRequest {

    /** ID session từ FE (uuid hoặc tracking id), bắt buộc. */
    @NotNull
    private String sessionId;

    /** Sản phẩm user vừa xem trong session (mới nhất → cũ nhất, max ~20 cái). */
    @NotEmpty
    private List<Long> recentProductIds;

    /** (Tùy chọn) Sản phẩm trong giỏ — sẽ loại khỏi gợi ý. */
    private List<Long> cartProductIds;

    /** (Tùy chọn) Số lượng kết quả mong muốn. */
    private Integer limit;
}