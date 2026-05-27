package com.ndh.ShopTechnology.dto.request.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateCollectorLogRequest {

    @NotBlank(message = "Event is required")
    private String event;

    private String sessionId;

    private String deviceType;

    private String platform;

    private String metadata;

    private String ipAddress;

    private Date timestamp;

    private Long productId;

    private Long userId;
}
