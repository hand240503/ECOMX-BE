package com.ndh.ShopTechnology.dto.request.user;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminModUserInfoRequest {

    private Long id;

    private String password;
    private String email;
    private String phoneNumber;
    private Integer status;
    private Integer type;

    private Long manId;

    private String fullName;
    private String telephone;
    private String avatar;
    private Long managerId;

    private String info01;
    private String info02;
    private String info03;
    private String info04;

    private Long roleId;

    private List<Integer> grantPermissionCodes;

    private List<Integer> revokePermissionCodes;

    private LocalDateTime permissionGrantExpiresAt;
}
