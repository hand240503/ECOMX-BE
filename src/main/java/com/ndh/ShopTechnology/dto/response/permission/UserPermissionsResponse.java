package com.ndh.ShopTechnology.dto.response.permission;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPermissionsResponse {

    private Long userId;
    private String username;

    private Set<String> roles;

    private Set<Integer> rolePermissions;

    private Set<Integer> userPermissions;

    private Set<Integer> effectivePermissions;
}
