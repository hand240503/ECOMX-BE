package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.enums.task.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "KanbanColumn")
@Table(name = "kanban_column",
    uniqueConstraints = @UniqueConstraint(name = "uk_col_board_status",
        columnNames = {"board_id", "status_key"}))
public class KanbanColumnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_key", nullable = false, length = 30)
    private TaskStatus statusKey;

    @Column(name = "label", nullable = false, length = 80)
    private String label;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(name = "wip_limit")
    private Integer wipLimit;
}
