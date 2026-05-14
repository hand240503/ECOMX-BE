package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.constants.RoleConstant;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolve {@link RoleEntity} từ id; fallback về role CUSTOMER nếu không truyền id / không tìm thấy (flow admin).
 */
@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    private final RoleRepository roleRepository;

    public RoleEntity assignRoleForPrivilegedCreator(Long roleId) {
        if (roleId != null) {
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> new NotFoundEntityException("Role not found: id=" + roleId));
        }
        return roleRepository.findByCode(RoleConstant.ROLE_CUSTOMER)
                .orElseThrow(() -> new NotFoundEntityException("No valid roles found. Please check role configuration."));
    }
}
