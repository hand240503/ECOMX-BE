package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.config.MyUser;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)  // ✅ Thêm transaction để load lazy relationships
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity userEntity = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Build authorities từ roles và permissions
        List<GrantedAuthority> authorities = buildAuthorities(userEntity);

        // Tạo MyUser với authorities
        MyUser myUserDetail = new MyUser(
                userEntity.getUsername(),
                userEntity.getPassword(),
                true,  // enabled
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                true,  // accountNonLocked
                authorities
        );

        // Copy properties
        BeanUtils.copyProperties(userEntity, myUserDetail);

        return myUserDetail;
    }

    /**
     * Build authorities từ roles và permissions của user
     */
    private List<GrantedAuthority> buildAuthorities(UserEntity user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Thêm ROLE authorities từ roles
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.getRoles().forEach(role -> {
                // Thêm role authority (VD: ROLE_ADMIN, ROLE_USER)
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));

                // 2. Thêm permission authorities từ role (VD: user:read, user:create)
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission ->
                            authorities.add(new SimpleGrantedAuthority(permission.getCode()))
                    );
                }
            });
        }

        // 3. Thêm user-specific permissions (override)
        if (user.getUserPermissions() != null) {
            user.getUserPermissions().forEach(userPerm -> {
                String permCode = userPerm.getPermission().getCode();
                GrantedAuthority authority = new SimpleGrantedAuthority(permCode);

                if (userPerm.getPermissionType() == com.ndh.ShopTechnology.entities.user.UserPermissionEntity.PermissionType.GRANT) {
                    // Grant permission
                    if (!authorities.contains(authority)) {
                        authorities.add(authority);
                    }
                } else {
                    // Deny permission (remove if exists)
                    authorities.remove(authority);
                }
            });
        }

        return authorities;
    }
}