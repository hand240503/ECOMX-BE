package com.ndh.ShopTechnology.dto.request.recommendation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SessionProfileRequest {

    @NotNull
    private String sessionId;

    @NotEmpty
    private List<Long> recentProductIds;

    private List<Long> cartProductIds;

    private Integer limit;
}
