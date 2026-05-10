package com.ndh.ShopTechnology.config;

import com.ndh.ShopTechnology.constants.RolePermissionDefaults;
import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import com.ndh.ShopTechnology.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Seed các role mặc định (SUPER_ADMIN, ADMIN, MANAGER, EMPLOYEE, CUSTOMER) kèm permission code mặc định
 * (xem {@link RolePermissionDefaults}).
 *
 * <p>Idempotent: nếu role đã tồn tại, hợp permission mặc định vào tập hiện có (không xoá quyền tùy biến đã cấu hình).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RolePermissionBootstrapper {

    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedRolesAndPermissions() {
        for (Map.Entry<String, Set<Integer>> entry : RolePermissionDefaults.DEFAULTS.entrySet()) {
            String code = entry.getKey();
            Set<Integer> defaults = entry.getValue();
            String readableName = toReadableName(code);

            // 1) Tìm theo code chuẩn.
            RoleEntity role = roleRepository.findByCode(code).orElse(null);

            // 2) Fallback: row legacy có thể đang giữ name "Super Admin" / "Admin" / ... với code khác
            //    (vd `ROLE_ADMIN`, `SuperAdmin`, NULL, ...). Tìm theo name để re-attach thay vì insert mới
            //    (insert mới sẽ đụng UNIQUE(name)).
            if (role == null) {
                role = roleRepository.findByName(readableName)
                        .flatMap(r -> roleRepository.findById(r.getId()))
                        .orElse(null);

                if (role != null && !code.equals(role.getCode())) {
                    log.info("[RolePermissionBootstrapper] Aligning legacy role name=\"{}\" : code {} → {}",
                            readableName, role.getCode(), code);
                    role.setCode(code);
                }
            }

            // 3) Vẫn không thấy → tạo mới.
            if (role == null) {
                role = RoleEntity.builder()
                        .code(code)
                        .name(readableName)
                        .description("System role: " + code)
                        .status(SystemConstant.ACTIVE_STATUS)
                        .permissionCodes(new LinkedHashSet<>(defaults))
                        .build();
                roleRepository.save(role);
                log.info("[RolePermissionBootstrapper] Created role {} with default permissions {}", code, defaults);
                continue;
            }

            // 4) Có role rồi → đảm bảo các trường mặc định + merge permission code.
            boolean changed = false;

            if (role.getName() == null || role.getName().isBlank()) {
                role.setName(readableName);
                changed = true;
            }
            if (role.getDescription() == null || role.getDescription().isBlank()) {
                role.setDescription("System role: " + code);
                changed = true;
            }
            if (role.getStatus() == null) {
                role.setStatus(SystemConstant.ACTIVE_STATUS);
                changed = true;
            }

            // Merge: tạo Set mới rồi gán lại — bắt buộc với cột JSON để Hibernate phát hiện thay đổi.
            Set<Integer> existing = role.getPermissionCodes() != null
                    ? role.getPermissionCodes()
                    : new LinkedHashSet<>();
            Set<Integer> merged = new LinkedHashSet<>(existing);
            boolean permsChanged = merged.addAll(defaults);

            if (permsChanged || changed) {
                role.setPermissionCodes(merged);
                roleRepository.save(role);
                log.info("[RolePermissionBootstrapper] Synced role {} → permissions {}",
                        code, merged);
            } else {
                log.debug("[RolePermissionBootstrapper] Role {} already up-to-date", code);
            }
        }
    }

    private String toReadableName(String code) {
        if (code == null || code.isBlank()) return code;
        StringBuilder sb = new StringBuilder();
        for (String part : code.split("_")) {
            if (part.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
