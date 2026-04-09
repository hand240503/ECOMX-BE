package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.entities.user.UserInfoEntity;
import com.ndh.ShopTechnology.exception.AuthenticationFailedException;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.permission.PermissionService;
import com.ndh.ShopTechnology.services.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserAvatarService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final PermissionService permissionService;

    @Transactional
    public UserResponse uploadAvatar(MultipartFile file) {
        UserEntity currentUser = getCurrentUserEntity();
        UserInfoEntity info = currentUser.getOrCreateUserInfo();

        String oldPublicId = info.getAvatarPublicId();
        CloudinaryService.UploadResult uploaded = cloudinaryService.uploadAvatar(file);

        if (oldPublicId != null && !oldPublicId.isBlank()) {
            cloudinaryService.deleteByPublicId(oldPublicId);
        }

        info.setAvatar(uploaded.url());
        info.setAvatarPublicId(uploaded.publicId());
        currentUser.setUserInfo(info);

        UserEntity saved = userRepository.save(currentUser);
        permissionService.clearUserPermissionsCache(saved.getUsername());
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse deleteAvatar() {
        UserEntity currentUser = getCurrentUserEntity();
        UserInfoEntity info = currentUser.getUserInfo();
        if (info == null) {
            return UserResponse.fromEntity(currentUser);
        }

        String oldPublicId = info.getAvatarPublicId();
        if (oldPublicId != null && !oldPublicId.isBlank()) {
            cloudinaryService.deleteByPublicId(oldPublicId);
        }

        info.setAvatar(null);
        info.setAvatarPublicId(null);
        currentUser.setUserInfo(info);

        UserEntity saved = userRepository.save(currentUser);
        permissionService.clearUserPermissionsCache(saved.getUsername());
        return UserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    protected UserEntity getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationFailedException(MessageConstant.AUTH_FAILED);
        }
        String username = auth.getName();
        return userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new AuthenticationFailedException(MessageConstant.AUTH_FAILED));
    }
}
