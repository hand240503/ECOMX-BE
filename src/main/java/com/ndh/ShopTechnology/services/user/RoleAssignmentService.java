package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.entities.role.RoleEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    private final RoleRepository roleRepository;

    public Set<RoleEntity> assignRoles(Set<Long> roleIds) {
        Set<RoleEntity> roles = new HashSet<>();

        if (roleIds != null && !roleIds.isEmpty()) {
            List<RoleEntity> roleList = roleRepository.findAllById(roleIds);
            roles.addAll(roleList);
        }

        if (roles.isEmpty()) {
            roleRepository.findByCode("ROLE_USER").ifPresent(roles::add);
        }

        if (roles.isEmpty()) {
            throw new NotFoundEntityException("No valid roles found. Please check role configuration.");
        }

        return roles;
    }
}
