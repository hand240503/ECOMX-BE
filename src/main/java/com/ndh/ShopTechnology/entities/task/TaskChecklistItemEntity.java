package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskChecklistItem")
@Table(name = "task_checklist_item",
    indexes = @Index(name = "idx_tci_checklist", columnList = "checklist_id, position"))
public class TaskChecklistItemEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tci_checklist"))
    private TaskChecklistEntity checklist;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "is_done", nullable = false)
    @Builder.Default
    private Boolean isDone = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", foreignKey = @ForeignKey(name = "fk_tci_assignee"))
    private UserEntity assignee;

    @Column(name = "due_date")
    @Temporal(TemporalType.DATE)
    private Date dueDate;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(name = "completed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by", foreignKey = @ForeignKey(name = "fk_tci_completed_by"))
    private UserEntity completedBy;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
}
