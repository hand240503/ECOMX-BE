package com.ndh.ShopTechnology.services.permission;

import com.ndh.ShopTechnology.dto.request.permission.GrantPermissionRequest;
import com.ndh.ShopTechnology.dto.request.permission.RevokePermissionRequest;
import com.ndh.ShopTechnology.dto.request.role.UpsertRoleRequest;
import com.ndh.ShopTechnology.dto.response.permission.UserPermissionsResponse;
import com.ndh.ShopTechnology.dto.response.role.RoleResponse;

import java.util.List;

/**
 * API quản lý role và permission cấp thêm cho user.
 *
 * <p>Tất cả method đều yêu cầu actor đã đăng nhập. Việc kiểm tra quyền (Role Admin / UPDATE_USER,
 * không cấp quá quyền mình có, ...) được thực hiện trong implementation.
 */
public interface RolePermissionService {

    // ===================== ROLE MANAGEMENT =====================

    /** Liệt kê tất cả role kèm permission mặc định. */
    List<RoleResponse> listRoles();

    /** Tạo mới role. Yêu cầu Role Admin/SuperAdmin. Permission mặc định không được vượt quá quyền của actor. */
    RoleResponse createRole(UpsertRoleRequest request);

    /** Cập nhật role theo id. Yêu cầu Role Admin/SuperAdmin. */
    RoleResponse updateRole(Long roleId, UpsertRoleRequest request);

    /** Xoá role theo id. Yêu cầu Role Admin/SuperAdmin. */
    void deleteRole(Long roleId);

    // ===================== USER PERMISSION GRANTS =====================

    /** Lấy chi tiết quyền của 1 user (role + cấp thêm + hiệu lực). Yêu cầu READ_USER hoặc READ_ALL. */
    UserPermissionsResponse getUserPermissions(Long userId);

    /**
     * Cấp thêm quyền cho user. Yêu cầu UPDATE_USER + actor phải đang có TẤT CẢ các quyền cần cấp.
     * Customer (không có UPDATE_USER) sẽ bị từ chối.
     */
    UserPermissionsResponse grantPermissions(GrantPermissionRequest request);

    /** Thu hồi quyền cấp thêm. Yêu cầu UPDATE_USER. */
    UserPermissionsResponse revokePermissions(RevokePermissionRequest request);
}
