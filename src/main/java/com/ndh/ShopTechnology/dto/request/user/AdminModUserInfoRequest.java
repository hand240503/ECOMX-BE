package com.ndh.ShopTechnology.dto.request.user;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminModUserInfoRequest {

    private Long id;

    // Core fields
    private String password;
    private String email;
    private String phoneNumber;
    private Integer status;
    private Integer type;

    // User info fields
    private String fullName;
    private String telephone;
    private String avatar;
    private Long managerId;

    // Custom fields
    private String info01;
    private String info02;
    private String info03;
    private String info04;

    // Roles
    private Set<Long> roleIds;
}

