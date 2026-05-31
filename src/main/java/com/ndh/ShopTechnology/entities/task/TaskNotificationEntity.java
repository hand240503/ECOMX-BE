package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskNotification")
@Table(name = "task_notification",
    indexes = {
        @Index(name = "idx_tn_recipient_unread", columnList = "recipient_id, is_read, created_at"),
        @Index(name = "idx_tn_task",             columnList = "task_id")
    })
public class TaskNotificationEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tn_recipient"))
    private UserEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tn_task"))
    private TaskEntity task;

    @Column(name = "notification_type", nullable = false, length = 40)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", length = 500)
    private String body;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date readAt;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdAt = new Date();
}
