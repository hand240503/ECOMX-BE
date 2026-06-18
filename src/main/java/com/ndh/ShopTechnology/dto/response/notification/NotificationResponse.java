package com.ndh.ShopTechnology.dto.response.notification;

import com.ndh.ShopTechnology.entities.notification.NotificationEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private String type;
    private Long orderId;
    private Boolean isRead;
    private Date createdDate;

    public static NotificationResponse fromEntity(NotificationEntity e) {
        if (e == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .message(e.getMessage())
                .type(e.getType())
                .orderId(e.getOrderId())
                .isRead(Boolean.TRUE.equals(e.getIsRead()))
                .createdDate(e.getCreatedDate())
                .build();
    }
}
