package com.ndh.ShopTechnology.dto.response.delivery;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Khoảng cách đường đi (OSRM) từ địa chỉ đã geocode (Nominatim) tới kho mặc định.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShippingDistanceResponse {

    /** Quãng đường dọc mạng lộ, mét */
    private double distanceMeters;

    private double distanceKilometers;

    /** Thời gian ước tính theo profile driving, giây */
    private double durationSeconds;

    /** Địa chỉ chuẩn hóa từ Nominatim */
    private String resolvedAddress;

    private double originLatitude;
    private double originLongitude;

    private double warehouseLatitude;
    private double warehouseLongitude;

    /** Phí ship (VND) theo bảng cứng: 3 km đầu 15k + 3,5k/km tiếp theo. */
    private Long shippingFeeVnd;
}
