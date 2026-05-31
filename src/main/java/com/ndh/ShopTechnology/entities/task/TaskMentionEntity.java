package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskMention")
@Table(name = "task_mention",
    uniqueConstraints = @UniqueConstraint(name = "uk_mention",
        columnNames = {"comment_id", "mentioned_user_id"}),
    indexes = @Index(name = "idx_tm_user_unread", columnList = "mentioned_user_id, is_read"))
public class TaskMentionEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_tm_comment"))
    private TaskCommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentioned_user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_tm_user"))
    private UserEntity mentionedUser;

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
