package com.ndh.ShopTechnology.dto.request.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserRequest {

    private String username;
    private String password;
    private String email;
    private String phoneNumber;

    private String fullName;
    private String telephone;
    private String avatar;
    private Long managerId;

    private Long manId;

    private String info01;
    private String info02;
    private String info03;
    private String info04;

    private Long roleId;
}
