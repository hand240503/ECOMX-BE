package com.ndh.ShopTechnology.dto.request.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModUserInfoRequest {

    private String fullName;
    private String telephone;
    private String avatar;
    private Long managerId;

    private String info01;
    private String info02;
    private String info03;
    private String info04;
}
