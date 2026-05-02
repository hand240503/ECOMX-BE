package com.ndh.ShopTechnology.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShippingFeeCalculatorTest {

    @Test
    void within3km_is15k() {
        assertEquals(15_000L, ShippingFeeCalculator.fromDistanceMeters(0.0));
        assertEquals(15_000L, ShippingFeeCalculator.fromDistanceMeters(2_999.0));
        assertEquals(15_000L, ShippingFeeCalculator.fromDistanceMeters(3_000.0));
    }

    /** 3,1 km: vượt 100 m → ceil 1 km phụ → 15k + 3,5k */
    @Test
    void km3_point1_is18_5k() {
        assertEquals(18_500L, ShippingFeeCalculator.fromDistanceMeters(3_100.0));
    }

    @Test
    void km4_exactly_oneExtraBlock() {
        assertEquals(18_500L, ShippingFeeCalculator.fromDistanceMeters(4_000.0));
    }

    @Test
    void km5_point2_threeExtraBlocks() {
        // 5,2 km → vượt 2,2 km → ceil = 3 → 15k + 3×3,5k = 25,5k
        assertEquals(25_500L, ShippingFeeCalculator.fromDistanceMeters(5_200.0));
    }

    @Test
    void nullDistance_returnsNull() {
        assertNull(ShippingFeeCalculator.fromDistanceMeters(null));
    }
}
