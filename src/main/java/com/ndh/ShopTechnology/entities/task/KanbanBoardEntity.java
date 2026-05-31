package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "KanbanBoard")
@Table(name = "kanban_board")
public class KanbanBoardEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
