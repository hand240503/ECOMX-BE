package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.enums.task.TaskParticipantRole;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskParticipant")
@Table(name = "task_participant",
    uniqueConstraints = @UniqueConstraint(name = "uk_tp_task_user_role",
        columnNames = {"task_id", "user_id", "role"}))
public class TaskParticipantEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tp_task"))
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tp_user"))
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private TaskParticipantRole role = TaskParticipantRole.PARTICIPANT;

    @Column(name = "assigned_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date assignedAt = new Date();

    @Column(name = "assigned_by")
    private Long assignedBy;
}
