package com.ndh.ShopTechnology.dto.response.delivery;

import lombok.*;

/**
 * Một lựa chọn store cho khách khi checkout: thông tin kho + khoảng cách tới địa
 * chỉ giao và phí ship tương ứng. {@code routable=false} nếu không định tuyến được
 * (thiếu toạ độ kho hoặc không tìm được đường) — khi đó distance/fee để null.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShippingStoreOptionResponse {
    private Long storeId;
    private String code;
    private String name;
    private String addressLine;
    private String city;
    private Double storeLatitude;
    private Double storeLongitude;
    private Boolean routable;
    private Double distanceMeters;
    private Double distanceKilometers;
    private Double durationSeconds;
    private Long shippingFeeVnd;
}
