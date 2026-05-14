package com.ndh.ShopTechnology.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private Integer status;
    private Integer type;

    /** ID user quản lý trực tiếp (users.man_id). */
    private Long manId;

    private UserInfoResponse userInfo;

    /** Tập role code (vd ADMIN, EMPLOYEE). */
    private Set<String> roles;

    /** Quyền hiệu lực (Integer) — đã hợp role + cấp thêm. */
    private Set<Integer> permissions;

    private UserAddressResponse defaultAddress;

    /**
     * Chỉ có khi tạo mới / reset mật khẩu: mật khẩu một lần (6 chữ số) để gửi nhân viên.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String temporaryPassword;

    public static UserResponse fromEntity(UserEntity entity) {
        if (entity == null) return null;

        UserResponseBuilder builder = UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .status(entity.getStatus())
                .type(entity.getType())
                .manId(entity.getManId());

        builder.userInfo(UserInfoResponse.fromEntity(entity.getUserInfo()));

        if (entity.getRole() != null) {
            LinkedHashSet<String> codes = new LinkedHashSet<>();
            codes.add(entity.getRole().getCode());
            builder.roles(codes);
        }

        builder.permissions(entity.getAllPermissions());

        if (entity.getAddresses() != null && !entity.getAddresses().isEmpty()) {
            entity.getAddresses().stream()
                    .filter(addr -> Boolean.TRUE.equals(addr.getIsDefault()))
                    .findFirst()
                    .ifPresent(addr ->
                            builder.defaultAddress(UserAddressResponse.fromEntity(addr))
                    );
        }

        return builder.build();
    }

    public static List<UserResponse> fromListEntity(List<UserEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
