package com.ndh.ShopTechnology.dto.response.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminActivityLogResponse {

    private Long id;
    private Long actorUserId;
    private String actorUsername;
    private String action;
    private String entityType;
    private Long entityId;
    private String entityLabel;
    private String snapshotBefore;
    private String snapshotAfter;
    private String ipAddress;
    private String userAgent;
    private Date createdAt;

    public static AdminActivityLogResponse fromEntity(AdminActivityLogEntity e) {
        if (e == null) return null;
        return AdminActivityLogResponse.builder()
                .id(e.getId())
                .actorUserId(e.getActorUser() != null ? e.getActorUser().getId() : null)
                .actorUsername(e.getActorUsername())
                .action(e.getAction())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .entityLabel(e.getEntityLabel())
                .snapshotBefore(e.getSnapshotBefore())
                .snapshotAfter(e.getSnapshotAfter())
                .ipAddress(e.getIpAddress())
                .userAgent(e.getUserAgent())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
