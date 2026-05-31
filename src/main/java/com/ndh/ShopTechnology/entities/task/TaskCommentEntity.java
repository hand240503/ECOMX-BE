package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskComment")
@Table(name = "task_comment",
    indexes = {
        @Index(name = "idx_tc_task",   columnList = "task_id, created_date"),
        @Index(name = "idx_tc_author", columnList = "author_id"),
        @Index(name = "idx_tc_parent", columnList = "parent_id")
    })
public class TaskCommentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tc_task"))
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tc_author"))
    private UserEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_tc_parent"))
    private TaskCommentEntity parent;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    @Column(name = "edited_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date editedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deletedAt;
}
