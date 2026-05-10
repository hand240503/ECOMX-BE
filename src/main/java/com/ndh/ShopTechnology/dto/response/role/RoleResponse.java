package com.ndh.ShopTechnology.dto.response.role;

import com.ndh.ShopTechnology.entities.role.RoleEntity;
import lombok.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer status;
    private Set<Integer> permissionCodes;

    public static RoleResponse fromEntity(RoleEntity role) {
        if (role == null) return null;
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .status(role.getStatus())
                .permissionCodes(role.getPermissionCodes() != null
                        ? new LinkedHashSet<>(role.getPermissionCodes())
                        : Collections.emptySet())
                .build();
    }

    public static List<RoleResponse> fromList(List<RoleEntity> roles) {
        if (roles == null) return Collections.emptyList();
        return roles.stream().map(RoleResponse::fromEntity).collect(Collectors.toList());
    }
}
