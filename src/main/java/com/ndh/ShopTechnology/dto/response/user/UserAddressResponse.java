package com.ndh.ShopTechnology.dto.response.user;

import com.ndh.ShopTechnology.entities.user.AddressType;
import com.ndh.ShopTechnology.entities.user.UserAddressEntity;
import com.ndh.ShopTechnology.utils.ShippingFeeCalculator;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAddressResponse {
    private Long id;
    private AddressType addressType;
    private String addressLine;
    private String city;
    private String state;
    private String country;
    private String zipCode;
    private Boolean isDefault;
    private Double latitude;
    private Double longitude;
    /** Quãng đường lái xe tới kho (m); null nếu chưa tính được (OSRM lỗi). */
    private Double distanceToWarehouseMeters;

    /** Phí ship ước tính (VND); null nếu không có khoảng cách. */
    private Long shippingFeeVnd;

    public static UserAddressResponse fromEntity(UserAddressEntity entity) {
        if (entity == null) return null;

        Double dist = entity.getDistanceToWarehouseMeters();
        Long fee = null;
        if (entity.getAddressType() == AddressType.USER) {
            fee = entity.getShippingFeeVnd();
            if (fee == null) {
                fee = ShippingFeeCalculator.fromDistanceMeters(dist);
            }
        }
        return UserAddressResponse.builder()
                .id(entity.getId())
                .addressType(entity.getAddressType())
                .addressLine(entity.getAddressLine())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .zipCode(entity.getZipCode())
                .isDefault(entity.getIsDefault())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .distanceToWarehouseMeters(dist)
                .shippingFeeVnd(fee)
                .build();
    }
}