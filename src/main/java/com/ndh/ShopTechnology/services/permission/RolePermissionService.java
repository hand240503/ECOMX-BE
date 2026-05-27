package com.ndh.ShopTechnology.services.permission;

import com.ndh.ShopTechnology.dto.request.permission.GrantPermissionRequest;
import com.ndh.ShopTechnology.dto.request.permission.RevokePermissionRequest;
import com.ndh.ShopTechnology.dto.request.role.UpsertRoleRequest;
import com.ndh.ShopTechnology.dto.response.permission.UserPermissionsResponse;
import com.ndh.ShopTechnology.dto.response.role.RoleResponse;

import java.util.List;

public interface RolePermissionService {

    List<RoleResponse> listRoles();

    RoleResponse createRole(UpsertRoleRequest request);

    RoleResponse updateRole(Long roleId, UpsertRoleRequest request);

    void deleteRole(Long roleId);

    UserPermissionsResponse getUserPermissions(Long userId);

    UserPermissionsResponse grantPermissions(GrantPermissionRequest request);

    UserPermissionsResponse revokePermissions(RevokePermissionRequest request);
}
