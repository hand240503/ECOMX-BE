package com.ndh.ShopTechnology.dto.response.store;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreResponse {
    private Long id;
    private String code;
    private String name;
    private String phone;
    private String addressLine;
    private String city;
    private Double latitude;
    private Double longitude;
    private Boolean active;
    private Boolean isDefault;
    private String note;
    private Date createdDate;
    private Date modifiedDate;
}
