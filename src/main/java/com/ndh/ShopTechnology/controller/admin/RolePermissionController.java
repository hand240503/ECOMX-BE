package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.constants.PermissionDescriptions;
import com.ndh.ShopTechnology.dto.request.permission.GrantPermissionRequest;
import com.ndh.ShopTechnology.dto.request.permission.RevokePermissionRequest;
import com.ndh.ShopTechnology.dto.request.role.UpsertRoleRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.permission.UserPermissionsResponse;
import com.ndh.ShopTechnology.dto.response.role.RoleResponse;
import com.ndh.ShopTechnology.services.permission.PermissionService;
import com.ndh.ShopTechnology.services.permission.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints quản lý role và permission.
 *
 * <p>Mỗi endpoint mở đầu bằng một call vào {@link PermissionService#requireAnyPermission(int...)}
 * hoặc {@code @PreAuthorize("@perm.check(...)")} (single code) để kiểm tra quyền — xem {@link PermissionCode}.
 * Customer không truy cập được do không được seed quyền nào trong nhóm này.
 */
@RestController
@RequestMapping("${api.prefix}/admin")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final PermissionService permissionService;

    // ===================== ROLES =====================

    @GetMapping("/roles")
    public ResponseEntity<APIResponse<List<RoleResponse>>> listRoles() {
        permissionService.requireAnyPermission(PermissionCode.MANAGE_ROLE, PermissionCode.READ_ALL);
        List<RoleResponse> roles = rolePermissionService.listRoles();
        return ResponseEntity.ok(APIResponse.of(true, "Roles retrieved successfully", roles, null, null));
    }

    @PostMapping("/roles")
    @PreAuthorize("@perm.check(111)")
    public ResponseEntity<APIResponse<RoleResponse>> createRole(@Valid @RequestBody UpsertRoleRequest request) {
        RoleResponse role = rolePermissionService.createRole(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Role created successfully", role, null, null));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("@perm.check(111)")
    public ResponseEntity<APIResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpsertRoleRequest request) {
        RoleResponse role = rolePermissionService.updateRole(id, request);
        return ResponseEntity.ok(APIResponse.of(true, "Role updated successfully", role, null, null));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("@perm.check(111)")
    public ResponseEntity<APIResponse<Void>> deleteRole(@PathVariable Long id) {
        rolePermissionService.deleteRole(id);
        return ResponseEntity.ok(APIResponse.of(true, "Role deleted successfully", null, null, null));
    }

    // ===================== USER PERMISSIONS =====================

    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<APIResponse<UserPermissionsResponse>> getUserPermissions(@PathVariable Long userId) {
        permissionService.requireAnyPermission(
                PermissionCode.GRANT_PERMISSION, PermissionCode.READ_USER, PermissionCode.READ_ALL);
        UserPermissionsResponse data = rolePermissionService.getUserPermissions(userId);
        return ResponseEntity.ok(APIResponse.of(true, "User permissions retrieved", data, null, null));
    }

    @PostMapping("/users/permissions/grant")
    @PreAuthorize("@perm.check(112)")
    public ResponseEntity<APIResponse<UserPermissionsResponse>> grantPermissions(
            @Valid @RequestBody GrantPermissionRequest request) {
        UserPermissionsResponse data = rolePermissionService.grantPermissions(request);
        return ResponseEntity.ok(APIResponse.of(true, "Permissions granted", data, null, null));
    }

    @PostMapping("/users/permissions/revoke")
    @PreAuthorize("@perm.check(112)")
    public ResponseEntity<APIResponse<UserPermissionsResponse>> revokePermissions(
            @Valid @RequestBody RevokePermissionRequest request) {
        UserPermissionsResponse data = rolePermissionService.revokePermissions(request);
        return ResponseEntity.ok(APIResponse.of(true, "Permissions revoked", data, null, null));
    }

    // ===================== METADATA =====================

    /**
     * Trả về catalog các permission code đã khai báo trong hệ thống. Mỗi entry kèm {@code description} để FE
     * render màn cấp quyền (tooltip / nhãn đầy đủ).
     */
    @GetMapping("/permissions/catalog")
    public ResponseEntity<APIResponse<Map<String, Object>>> permissionCatalog() {
        permissionService.requireAnyPermission(
                PermissionCode.MANAGE_ROLE, PermissionCode.GRANT_PERMISSION, PermissionCode.READ_ALL);
        List<Map<String, Object>> systemWide = List.of(
                describeEntry(PermissionCode.CREATE_ALL,        "Create all"),
                describeEntry(PermissionCode.READ_ALL,          "Read all"),
                describeEntry(PermissionCode.UPDATE_ALL,        "Update all"),
                describeEntry(PermissionCode.DELETE_ALL,        "Delete all"),
                describeEntry(PermissionCode.LOCK_USER,         "Lock user"),
                describeEntry(PermissionCode.MANAGE_ROLE,       "Manage role"),
                describeEntry(PermissionCode.GRANT_PERMISSION,  "Grant permission")
        );

        List<Map<String, Object>> moduleSpecific = PermissionCode.allKnownCodes().stream()
                .filter(PermissionCode::isModuleSpecific)
                .map(c -> describeEntry(c, labelFor(c)))
                .toList();

        Map<String, Object> data = Map.of(
                "systemWide",     systemWide,
                "moduleSpecific", moduleSpecific,
                "modules", Map.of(
                        "PRODUCT",  PermissionCode.MODULE_PRODUCT,
                        "PRICE",    PermissionCode.MODULE_PRICE,
                        "UNIT",     PermissionCode.MODULE_UNIT,
                        "BRAND",    PermissionCode.MODULE_BRAND,
                        "CATEGORY", PermissionCode.MODULE_CATEGORY,
                        "DOCUMENT", PermissionCode.MODULE_DOCUMENT,
                        "EMPLOYEE", PermissionCode.MODULE_EMPLOYEE,
                        "ORDER",    PermissionCode.MODULE_ORDER,
                        "REPORT",   PermissionCode.MODULE_REPORT,
                        "USER",     PermissionCode.MODULE_USER
                ),
                "actions", Map.of(
                        "CREATE", PermissionCode.ACTION_CREATE,
                        "READ",   PermissionCode.ACTION_READ,
                        "UPDATE", PermissionCode.ACTION_UPDATE,
                        "DELETE", PermissionCode.ACTION_DELETE
                ),
                "allKnownCodes", PermissionCode.allKnownCodes()
        );
        return ResponseEntity.ok(APIResponse.of(true, "Permission catalog", data, null, null));
    }

    private static Map<String, Object> describeEntry(int code, String label) {
        return Map.of(
                "code", code,
                "label", label,
                "description", PermissionDescriptions.describe(code)
        );
    }

    /**
     * Sinh label kiểu "Action Module" từ mã 6 chữ số. Vd 100002 → "READ PRODUCT".
     */
    private static String labelFor(int code) {
        if (!PermissionCode.isModuleSpecific(code)) return String.valueOf(code);
        int module = PermissionCode.extractModule(code);
        int action = PermissionCode.extractAction(code);
        String moduleName = switch (module) {
            case PermissionCode.MODULE_PRODUCT  -> "PRODUCT";
            case PermissionCode.MODULE_PRICE     -> "PRICE";
            case PermissionCode.MODULE_UNIT      -> "UNIT";
            case PermissionCode.MODULE_BRAND     -> "BRAND";
            case PermissionCode.MODULE_CATEGORY -> "CATEGORY";
            case PermissionCode.MODULE_DOCUMENT -> "DOCUMENT";
            case PermissionCode.MODULE_EMPLOYEE -> "EMPLOYEE";
            case PermissionCode.MODULE_ORDER    -> "ORDER";
            case PermissionCode.MODULE_REPORT   -> "REPORT";
            case PermissionCode.MODULE_USER     -> "USER";
            default -> "MODULE_" + module;
        };
        String actionName = switch (action) {
            case PermissionCode.ACTION_CREATE -> "CREATE";
            case PermissionCode.ACTION_READ   -> "READ";
            case PermissionCode.ACTION_UPDATE -> "UPDATE";
            case PermissionCode.ACTION_DELETE -> "DELETE";
            default -> "ACTION_" + action;
        };
        return actionName + " " + moduleName;
    }
}
