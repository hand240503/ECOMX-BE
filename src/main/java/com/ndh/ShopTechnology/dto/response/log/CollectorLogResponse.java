package com.ndh.ShopTechnology.dto.response.log;

import com.ndh.ShopTechnology.entities.log.CollectorLogEntity;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollectorLogResponse {

    private Long id;
    private String event;
    private String sessionId;
    private String deviceType;
    private String platform;
    private String metadata;
    private String ipAddress;
    private Date timestamp;
    private Long productId;
    private String productName;
    private Long userId;
    private String username;
    private Date createdDate;
    private Date modifiedDate;

    public static CollectorLogResponse fromEntity(CollectorLogEntity entity) {
        if (entity == null)
            return null;

        CollectorLogResponseBuilder builder = CollectorLogResponse.builder()
                .id(entity.getId())
                .event(entity.getEvent())
                .sessionId(entity.getSessionId())
                .deviceType(entity.getDeviceType())
                .platform(entity.getPlatform())
                .metadata(entity.getMetadata())
                .ipAddress(entity.getIpAddress())
                .timestamp(entity.getTimestamp())
                .createdDate(entity.getCreatedDate())
                .modifiedDate(entity.getModifiedDate());

        if (entity.getProduct() != null) {
            builder.productId(entity.getProduct().getId())
                    .productName(entity.getProduct().getProductName());
        }

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .username(entity.getUser().getUsername());
        }

        return builder.build();
    }
}
