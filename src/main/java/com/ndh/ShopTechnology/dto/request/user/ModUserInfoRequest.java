package com.ndh.ShopTechnology.dto.request.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModUserInfoRequest {

    private String fullName;
    private String telephone; // Optional, might be used as alternative or extra
    private String email;
    private String phoneNumber;
    private String avatar;
    private Long managerId;

    private String info01;
    private String info02;
    private String info03;
    private String info04;
}
