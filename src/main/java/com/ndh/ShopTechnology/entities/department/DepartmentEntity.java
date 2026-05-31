package com.ndh.ShopTechnology.entities.department;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "departments")
public class DepartmentEntity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Mã màu hex hiển thị trên FE, ví dụ #3B82F6 */
    @Column(name = "color", length = 20)
    private String color;

    /**
     * Danh sách permission code được cấp cho thành viên phòng ban.
     * Lưu dạng CSV ("100002,500002,700002") để tránh join table phức tạp.
     */
    @Column(name = "permission_codes_csv", columnDefinition = "TEXT")
    private String permissionCodesCsv;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 1; // 1=active, 0=inactive

    /** Thành viên thuộc phòng ban */
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserDepartmentEntity> members = new HashSet<>();

    /** Parse permissionCodesCsv → Set<Integer> */
    @Transient
    public Set<Integer> getPermissionCodes() {
        Set<Integer> codes = new HashSet<>();
        if (permissionCodesCsv == null || permissionCodesCsv.isBlank()) return codes;
        for (String part : permissionCodesCsv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try { codes.add(Integer.parseInt(trimmed)); } catch (NumberFormatException ignored) {}
            }
        }
        return codes;
    }
}
