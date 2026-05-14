# -*- coding: utf-8 -*-
"""Generate docs/PERMISSION_SOURCECODE.md with concatenated Java sources."""
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
SRC = BASE / "src/main/java/com/ndh/ShopTechnology"
OUT = BASE / "docs/PERMISSION_SOURCECODE.md"

FILES = [
    "constants/PermissionCode.java",
    "constants/PermissionDescriptions.java",
    "constants/RolePermissionDefaults.java",
    "constants/RoleConstant.java",
    "entities/role/RoleEntity.java",
    "entities/user/UserEntity.java",
    "entities/user/UserPermissionEntity.java",
    "repository/UserRepository.java",
    "repository/UserPermissionRepository.java",
    "dto/request/permission/GrantPermissionRequest.java",
    "dto/request/permission/RevokePermissionRequest.java",
    "dto/response/permission/UserPermissionsResponse.java",
    "dto/request/role/UpsertRoleRequest.java",
    "dto/response/role/RoleResponse.java",
    "dto/request/user/AdminModUserInfoRequest.java",
    "services/permission/PermissionEvaluator.java",
    "services/permission/PermissionService.java",
    "services/permission/PermissionAuthorizationBean.java",
    "services/permission/RolePermissionService.java",
    "services/permission/impl/RolePermissionServiceImpl.java",
    "config/RolePermissionBootstrapper.java",
    "config/CacheConfig.java",
    "services/user/CustomUserDetailsService.java",
    "services/user/UserAvatarService.java",
    "controller/admin/RolePermissionController.java",
    "controller/admin/AdminCustomerController.java",
    "controller/admin/AdminStaffController.java",
    "controller/admin/AdminEmployeeController.java",
    "controller/admin/AdminProductController.java",
    "controller/product/ProductController.java",
    "controller/admin/AdminCategoryController.java",
    "controller/category/CategoryController.java",
    "controller/admin/AdminBrandController.java",
    "controller/admin/AdminUnitController.java",
    "controller/admin/AdminDocumentController.java",
    "controller/document/DocumentController.java",
    "controller/admin/AdminProductPriceController.java",
    "controller/admin/AdminProductPriceChangeController.java",
    "controller/admin/AdminVolumePriceTierController.java",
    "controller/admin/AdminPurchaseWithPurchaseController.java",
    "controller/job/JobReportController.java",
    "config/WebSecurityConfig.java",
    "services/user/impl/UserServiceImpl.java",
]

INTRO = """# Tài liệu mã nguồn hệ thống quyền (Permission)

File được **tạo tự động** bởi `scripts/gen_permission_source_md.py` — gộp các file Java liên quan permission, role grants và kiểm tra quyền trên API.

**Chạy lại sau khi sửa code:** `python scripts/gen_permission_source_md.py`

## Mục lục

1. [Danh sách file trong bundle](#danh-sách-file-trong-bundle)
2. [Bảng tra cứu nhanh](#bảng-tra-cứu-nhanh)
3. [Mã nguồn](#mã-nguồn)

## Danh sách file trong bundle

"""


def main() -> None:
    lines = [INTRO]
    for rel in FILES:
        lines.append(f"- `{rel}`\n")
    lines.append("\n## Bảng tra cứu nhanh\n\n")
    lines.append("| Khu vực | Ghi chú |\n")
    lines.append("|---------|--------|\n")
    lines.append("| Constants | `PermissionCode`, `PermissionDescriptions`, `RolePermissionDefaults`, `RoleConstant` |\n")
    lines.append("| Persistence | `RoleEntity.permissionCodes`, `UserPermissionEntity`, `UserRepository` fetch grants |\n")
    lines.append("| Services | `PermissionEvaluator`, `PermissionService`, `RolePermissionServiceImpl`, `UserServiceImpl` |\n")
    lines.append("| SpEL | `@Component(\"perm\")` → `@PreAuthorize(\"@perm.check(code)\")` |\n")
    lines.append("| Security | `WebSecurityConfig`: `@EnableMethodSecurity` |\n")
    lines.append("| Bootstrap | `RolePermissionBootstrapper` seed role |\n")
    lines.append("| Controllers | `RolePermissionController`, admin CRUD + `ProductController` + `CategoryController` + `JobReportController` + `DocumentController` |\n")
    lines.append("\n## Mã nguồn\n\n")
    text = "".join(lines)

    for rel in FILES:
        path = SRC / rel
        if not path.is_file():
            raise SystemExit(f"Missing file: {path}")
        content = path.read_text(encoding="utf-8")
        text += f"### `{rel}`\n\n```java\n{content}\n```\n\n"

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text, encoding="utf-8")
    print(f"Wrote {OUT} ({len(text)} chars)")


if __name__ == "__main__":
    main()
