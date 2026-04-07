package com.ndh.ShopTechnology.dto.response.user;

import com.ndh.ShopTechnology.entities.user.UserInfoEntity;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoResponse {

    private String fullName;
    private String telephone;
    private String avatar;
    private Long managerId;
    private String info01;
    private String info02;
    private String info03;
    private String info04;

    public static UserInfoResponse fromEntity(UserInfoEntity entity) {
        if (entity == null) return null;

        return UserInfoResponse.builder()
                .fullName(entity.getFullName())
                .telephone(entity.getTelephone())
                .avatar(entity.getAvatar())
                .managerId(entity.getManagerId())
                .info01(entity.getInfo01())
                .info02(entity.getInfo02())
                .info03(entity.getInfo03())
                .info04(entity.getInfo04())
                .build();
    }
}
