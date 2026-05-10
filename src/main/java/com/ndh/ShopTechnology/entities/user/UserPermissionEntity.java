package com.ndh.ShopTechnology.entities.user;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Quyền cấp <b>thêm</b> cho user (ngoài permission mặc định của role mà user đó đang có).
 *
 * <p>Quy tắc:
 * <ul>
 *   <li>Permission code ở đây luôn được <b>cộng dồn</b> với permission của role: <code>Effective = Role + User</code>.</li>
 *   <li>Một user không thể có 2 row trùng {@code permissionCode} (đã ràng buộc unique).</li>
 *   <li>Trường {@code expiresAt} (tuỳ chọn) cho phép cấp tạm thời — quyền tự hết hạn theo thời gian.</li>
 *   <li>Trường {@code assignedBy} ghi nhận username của người đã cấp quyền (để audit).</li>
 * </ul>
 */
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

    /**
     * Permission code (Integer). Phải tuân theo quy tắc trong {@code PermissionCode}.
     */
    @Column(name = "permission_code", nullable = false)
    private Integer permissionCode;

    /** Username của người cấp quyền. Có thể null nếu được seed bởi hệ thống. */
    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    /** Hết hạn (null = vô thời hạn). */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
