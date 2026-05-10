package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.config.MyUser;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.entities.user.UserPermissionEntity;
import com.ndh.ShopTechnology.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Build authorities từ role + permission code (Integer) của user.
 *
 * <p>Ánh xạ:
 * <ul>
 *   <li>Role code → authority {@code ROLE_<code>} (vd ADMIN → ROLE_ADMIN). Dùng cho {@code hasRole(...)}.</li>
 *   <li>Permission code (Integer) → authority {@code PERM_<code>} (vd 100002 → PERM_100002). Dùng cho debug/log; logic check chính thực hiện qua bean {@code @perm.check(...)}.</li>
 * </ul>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    public static final String AUTHORITY_PERM_PREFIX = "PERM_";
    public static final String AUTHORITY_ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity userEntity = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = buildAuthorities(userEntity);

        MyUser myUserDetail = new MyUser(
                userEntity.getUsername(),
                userEntity.getPassword(),
                true,
                true,
                true,
                true,
                authorities
        );

        BeanUtils.copyProperties(userEntity, myUserDetail);

        return myUserDetail;
    }

    /**
     * Build authority list = role authorities (ROLE_*) + permission authorities (PERM_*).
     */
    private List<GrantedAuthority> buildAuthorities(UserEntity user) {
        Set<Integer> effective = new LinkedHashSet<>();
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                authorities.add(new SimpleGrantedAuthority(AUTHORITY_ROLE_PREFIX + role.getCode()));
                if (role.getPermissionCodes() != null) {
                    effective.addAll(role.getPermissionCodes());
                }
            });
        }

        if (user.getUserPermissions() != null) {
            LocalDateTime now = LocalDateTime.now();
            for (UserPermissionEntity up : user.getUserPermissions()) {
                if (up.getPermissionCode() == null) continue;
                if (up.getExpiresAt() != null && up.getExpiresAt().isBefore(now)) continue;
                effective.add(up.getPermissionCode());
            }
        }

        for (Integer code : effective) {
            authorities.add(new SimpleGrantedAuthority(AUTHORITY_PERM_PREFIX + code));
        }

        return authorities;
    }
}
