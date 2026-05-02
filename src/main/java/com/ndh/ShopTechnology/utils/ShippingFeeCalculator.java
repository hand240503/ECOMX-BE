package com.ndh.ShopTechnology.utils;

/**
 * Phí ship cứng theo quãng đường lái (mét) tới kho:
 * <ul>
 *   <li>Trong vòng 3 km đầu (≤ 3.000 m): <strong>15.000 ₫</strong></li>
 *   <li>Phần vượt 3 km: mỗi km (hoặc phần km) tính <strong>+3.500 ₫</strong>, <strong>làm tròn lên</strong> theo từng km</li>
 * </ul>
 * Ví dụ: 3,1 km → vượt 0,1 km vẫn tính thêm 1 bậc km → 15.000 + 3.500 = <strong>18.500 ₫</strong>.
 */
public final class ShippingFeeCalculator {

    /** Quãng đường gộp trong mức phí nền (3 km đầu). */
    private static final int BASE_INCLUDED_METERS = 3_000;
    private static final long BASE_FEE_VND = 15_000L;
    private static final long PER_EXTRA_KM_VND = 3_500L;

    private ShippingFeeCalculator() {}

    /**
     * @param distanceMeters khoảng cách tới kho (OSRM), có thể null nếu chưa tính được
     * @return phí VND, hoặc null nếu không có khoảng cách
     */
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
