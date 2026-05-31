package com.ndh.ShopTechnology.entities.department;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "user_departments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "department_id"})
)
public class UserDepartmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    /**
     * Vị trí trong phòng ban: LEADER hoặc MEMBER.
     * Mỗi phòng ban chỉ có 1 LEADER.
     */
    @Column(name = "position", nullable = false, length = 20)
    @Builder.Default
    private String position = "MEMBER";

    /** Người thực hiện gán */
    @Column(name = "assigned_by", length = 100)
    private String assignedBy;
}
