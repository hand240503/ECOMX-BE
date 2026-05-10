package com.ndh.ShopTechnology.entities.role;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Role của hệ thống. Mỗi role có một danh sách <b>permission code mặc định</b> (số nguyên)
 * được lưu thẳng trên cột {@code permission_codes} (JSON) của bảng {@code roles}.
 *
 * <p>Ví dụ row trong DB:
 * <pre>
 *   id | code        | name        | permission_codes
 *   1  | SUPER_ADMIN | Super Admin | [101,102,103,104,110,111,112]
 *   2  | ADMIN       | Admin       | [101,102,103,104,112]
 * </pre>
 *
 * <p>Mã quyền tuân theo {@link com.ndh.ShopTechnology.constants.PermissionCode}:
 * <ul>
 *   <li>3 chữ số → áp dụng toàn hệ thống.</li>
 *   <li>6 chữ số → áp dụng cho 1 module cụ thể (MMMAAA).</li>
 * </ul>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class RoleEntity extends BaseEntity {

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    private Integer status;

    /**
     * Danh sách permission mặc định, lưu thẳng trên 1 cột JSON (vd: {@code [101,102,103]}).
     *
     * <p>Dùng {@link LinkedHashSet} để ổn định thứ tự khi serialize / log; tránh trùng lặp ngay tại app.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permission_codes", columnDefinition = "json")
    @Builder.Default
    private Set<Integer> permissionCodes = new LinkedHashSet<>();
}
