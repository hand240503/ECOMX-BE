package com.ndh.ShopTechnology.dto.response.delivery;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShippingDistanceResponse {

    private double distanceMeters;

    private double distanceKilometers;

    private double durationSeconds;

    private String resolvedAddress;

    private double originLatitude;
    private double originLongitude;

    private double warehouseLatitude;
    private double warehouseLongitude;

    private Long shippingFeeVnd;
}
