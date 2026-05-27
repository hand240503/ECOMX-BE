package com.ndh.ShopTechnology.utils;

public final class ShippingFeeCalculator {

    private static final int BASE_INCLUDED_METERS = 3_000;
    private static final long BASE_FEE_VND = 15_000L;
    private static final long PER_EXTRA_KM_VND = 3_500L;

    private ShippingFeeCalculator() {}

    public static Long fromDistanceMeters(Double distanceMeters) {
        if (distanceMeters == null || distanceMeters < 0 || Double.isNaN(distanceMeters)) {
            return null;
        }
        if (distanceMeters <= BASE_INCLUDED_METERS) {
            return BASE_FEE_VND;
        }
        double extraMeters = distanceMeters - BASE_INCLUDED_METERS;
        int extraKmBlocks = (int) Math.ceil(extraMeters / 1000.0);
        return BASE_FEE_VND + extraKmBlocks * PER_EXTRA_KM_VND;
    }
}
