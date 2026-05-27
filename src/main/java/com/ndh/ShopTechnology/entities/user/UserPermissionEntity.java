package com.ndh.ShopTechnology.entities.user;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "user_permission_grants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission_code"})
)
public class UserPermissionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "permission_code", nullable = false)
    private Integer permissionCode;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
