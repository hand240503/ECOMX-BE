package com.ndh.ShopTechnology.entities.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.permission.PermissionEntity;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Column(name = "email")
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
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @JsonIgnore
    private Set<RoleEntity> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<UserPermissionEntity> userPermissions = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<UserAddressEntity> addresses = new HashSet<>();

    // Helper method to access user info
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

    @Transient
    public Set<String> getAllPermissions() {
        Set<String> permissions = new HashSet<>();

        // Từ roles
        if (roles != null) {
            roles.forEach(role -> {
                if (role.getPermissions() != null) {
                    permissions.addAll(
                            role.getPermissions().stream()
                                    .map(PermissionEntity::getCode)
                                    .collect(Collectors.toSet())
                    );
                }
            });
        }

        if (userPermissions != null) {
            userPermissions.forEach(up -> {
                String permCode = up.getPermission().getCode();
                if (up.getPermissionType() == UserPermissionEntity.PermissionType.GRANT) {
                    permissions.add(permCode);
                } else {
                    permissions.remove(permCode);
                }
            });
        }

        return permissions;
    }

    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) return false;
        Set<String> userPerms = getAllPermissions();
        for (String perm : permissions) {
            if (userPerms.contains(perm)) return true;
        }
        return false;
    }

    public boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) return false;
        Set<String> userPerms = getAllPermissions();
        for (String perm : permissions) {
            if (!userPerms.contains(perm)) return false;
        }
        return true;
    }

    public boolean hasRole(String roleCode) {
        if (roles == null) return false;
        return roles.stream().anyMatch(r -> r.getCode().equals(roleCode));
    }

    public boolean hasAnyRole(String... roleCodes) {
        if (roleCodes == null || roleCodes.length == 0) return false;
        for (String roleCode : roleCodes) {
            if (hasRole(roleCode)) return true;
        }
        return false;
    }
}