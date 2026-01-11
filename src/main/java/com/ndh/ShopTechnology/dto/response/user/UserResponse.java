package com.ndh.ShopTechnology.dto.response.user;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import lombok.*;

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

    private UserInfoResponse userInfo;

    private Set<String> roles;
    private Set<String> permissions;

    private UserAddressResponse defaultAddress;

    public static UserResponse fromEntity(UserEntity entity) {
        if (entity == null) return null;

        UserResponseBuilder builder = UserResponse.builder()
                .id(entity.getId())
//                .username(entity.getUsername())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .status(entity.getStatus())
                .type(entity.getType());

        builder.userInfo(UserInfoResponse.fromEntity(entity.getUserInfo()));

        if (entity.getRoles() != null) {
            builder.roles(entity.getRoles().stream()
                    .map(role -> role.getCode())
                    .collect(Collectors.toSet()));
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
