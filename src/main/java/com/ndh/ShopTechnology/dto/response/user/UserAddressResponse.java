package com.ndh.ShopTechnology.dto.response.user;

import com.ndh.ShopTechnology.entities.user.UserAddressEntity;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAddressResponse {
    private Long id;
    private String addressLine;
    private String city;
    private String state;
    private String country;
    private String zipCode;
    private Boolean isDefault;

    public static UserAddressResponse fromEntity(UserAddressEntity entity) {
        if (entity == null) return null;

        return UserAddressResponse.builder()
                .id(entity.getId())
                .addressLine(entity.getAddressLine())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .zipCode(entity.getZipCode())
                .isDefault(entity.getIsDefault())
                .build();
    }
}