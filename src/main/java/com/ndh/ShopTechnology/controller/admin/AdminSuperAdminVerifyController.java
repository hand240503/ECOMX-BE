package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.constants.RoleConstant;
import com.ndh.ShopTechnology.dto.request.auth.VerifySuperAdminRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Xác thực mật khẩu của tài khoản SUPER_ADMIN cho thao tác nhạy cảm phía admin
 * (ví dụ: cổng ẩn xuất toàn bộ sản phẩm trong trang Sản phẩm).
 *
 * <p>Mật khẩu nhập vào được so khớp (BCrypt) với mật khẩu của <b>bất kỳ</b> tài khoản
 * mang role {@code SUPER_ADMIN}. Trả về {@code true} nếu khớp, ngược lại {@code false}.
 */
@RestController
@RequestMapping("${api.prefix}/admin/products")
public class AdminSuperAdminVerifyController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSuperAdminVerifyController(UserRepository userRepository,
                                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Kiểm tra mật khẩu có khớp với một tài khoản SUPER_ADMIN hay không. */
    @PostMapping("/verify-super-admin")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<Boolean>> verify(@Valid @RequestBody VerifySuperAdminRequest request) {
        String raw = request.getPassword();

        List<UserEntity> superAdmins = userRepository.findByRole_Code(RoleConstant.ROLE_SUPER_ADMIN);
        boolean matched = superAdmins.stream()
                .map(UserEntity::getPassword)
                .filter(pwd -> pwd != null && !pwd.isBlank())
                .anyMatch(pwd -> passwordEncoder.matches(raw, pwd));

        if (matched) {
            return ResponseEntity.ok(APIResponse.of(true, "OK", true, null, null));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(APIResponse.of(false, "Mật khẩu super admin không đúng", false, null, null));
    }
}
