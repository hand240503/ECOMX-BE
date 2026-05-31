package com.ndh.ShopTechnology.entities.task;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskChecklist")
@Table(name = "task_checklist",
    indexes = @Index(name = "idx_tcl_task", columnList = "task_id"))
public class TaskChecklistEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tcl_task"))
    private TaskEntity task;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdAt = new Date();

    @Column(name = "created_by", nullable = false, length = 100)
    @Builder.Default
    private String createdBy = "system";

    @OneToMany(mappedBy = "checklist", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<TaskChecklistItemEntity> items;
}
