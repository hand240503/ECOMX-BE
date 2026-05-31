package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.enums.task.TaskActivityAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Date;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskActivityLog")
@Table(name = "task_activity_log",
    indexes = {
        @Index(name = "idx_tal_task",   columnList = "task_id, created_at"),
        @Index(name = "idx_tal_actor",  columnList = "actor_id, created_at"),
        @Index(name = "idx_tal_action", columnList = "action_type, created_at")
    })
public class TaskActivityLogEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tal_task"))
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tal_actor"))
    private UserEntity actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private TaskActivityAction actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "JSON")
    private Map<String, Object> oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "JSON")
    private Map<String, Object> newValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdAt = new Date();
}
