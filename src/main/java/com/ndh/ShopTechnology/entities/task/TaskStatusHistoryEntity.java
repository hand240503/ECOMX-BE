package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.enums.task.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskStatusHistory")
@Table(name = "task_status_history",
    indexes = {
        @Index(name = "idx_tsh_task",      columnList = "task_id, changed_at"),
        @Index(name = "idx_tsh_to_status", columnList = "to_status, changed_at")
    })
public class TaskStatusHistoryEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tsh_task"))
    private TaskEntity task;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private TaskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private TaskStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false, foreignKey = @ForeignKey(name = "fk_tsh_changed_by"))
    private UserEntity changedBy;

    @Column(name = "changed_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date changedAt = new Date();

    @Column(name = "time_in_prev_status_minutes")
    private Integer timeInPrevStatusMinutes;

    @Column(name = "note", length = 500)
    private String note;
}
