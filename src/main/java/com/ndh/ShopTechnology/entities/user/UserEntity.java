package com.ndh.ShopTechnology.entities.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @NotNull(message = "Username không được để trống")
    @Column(name = "user_name", nullable = false, unique = true)
    private String username;

    @NotNull(message = "Password không được để trống")
    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "telephone", unique = true)
    private String phoneNumber;

    @Column(name = "status")
    private Integer status;

    @Column(name = "type")
    private Integer type;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private UserInfoEntity userInfo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIgnore
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private Set<UserPermissionEntity> userPermissions = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<UserAddressEntity> addresses = new HashSet<>();

    @Transient
    public UserInfoEntity getOrCreateUserInfo() {
        if (userInfo == null) {
            userInfo = new UserInfoEntity();
            userInfo.setUser(this);
        }
        return userInfo;
    }

    @Transient
    public UserAddressEntity getDefaultAddress() {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        return addresses.stream()
                .filter(addr -> addr.getIsDefault() != null && addr.getIsDefault())
                .findFirst()
                .orElse(null);
    }

    /**
     * Trả về tập hợp <b>permission code (Integer)</b> đầy đủ của user:
     * <pre>Effective = (Σ permissions của các role) ∪ (permissions cấp thêm cho user, chưa hết hạn)</pre>
     *
     * <p>Lưu ý: hàm này KHÔNG mở rộng wildcard 101..104 thành các permission 6 chữ số tương ứng.
     * Việc kiểm tra wildcard được thực hiện trong {@code PermissionEvaluator#hasPermission}.
     */
    @Transient
    public Set<Integer> getAllPermissions() {
        Set<Integer> permissions = new HashSet<>();

        if (roles != null) {
            for (RoleEntity role : roles) {
                if (role.getPermissionCodes() != null) {
                    permissions.addAll(role.getPermissionCodes());
                }
            }
        }

        if (userPermissions != null) {
            LocalDateTime now = LocalDateTime.now();
            for (UserPermissionEntity up : userPermissions) {
                if (up.getPermissionCode() == null) continue;
                if (up.getExpiresAt() != null && up.getExpiresAt().isBefore(now)) {
                    continue;
                }
                permissions.add(up.getPermissionCode());
            }
        }

        return permissions;
    }

    @Transient
    public boolean hasRole(String roleCode) {
        if (roles == null || roleCode == null) return false;
        return roles.stream().anyMatch(r -> roleCode.equals(r.getCode()));
    }

    @Transient
    public boolean hasAnyRole(String... roleCodes) {
        if (roleCodes == null || roleCodes.length == 0) return false;
        for (String roleCode : roleCodes) {
            if (hasRole(roleCode)) return true;
        }
        return false;
    }
}
