package com.ndh.ShopTechnology.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Địa chỉ kho mặc định + URL dịch vụ geocoding (Nominatim) và routing (OSRM).
 * Production: nên tự host OSRM/Nominatim hoặc dùng nhà cung cấp có SLA; public demo có giới hạn.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "delivery.routing")
public class DeliveryRoutingProperties {

    /** Vĩ độ kho giao hàng mặc định */
    private double warehouseLatitude = 15.975435631036287;

    /** Kinh độ kho giao hàng mặc định */
    private double warehouseLongitude = 108.25329136862204;

    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org";

    /** Base URL OSRM, ví dụ public demo (không dùng production tải cao). */
    private String osrmBaseUrl = "https://router.project-osrm.org";

    /** Ràng buộc tìm kiếm địa chỉ theo quốc gia (Nominatim {@code countrycodes}). */
    private String countryCodes = "vn";

    /**
     * Email liên hệ gửi trong header {@code From} tới Nominatim (yêu cầu trong
     * <a href="https://operations.osmfoundation.org/policies/nominatim/">usage policy</a>).
     */
    private String nominatimContactEmail = "";

    /**
     * Bắt buộc với Nominatim: User-Agent mô tả ứng dụng; nên chứa email hoặc kèm {@link #nominatimContactEmail}.
     */
    private String httpUserAgent = "ShopTechnology/1.0 (ecomx-be)";

    /**
     * Khi Nominatim trả 403/429 hoặc lỗi mạng, thử Photon (Komoot) — ít ràng buộc header hơn, phù hợp dev.
     */
    private boolean photonFallbackEnabled = true;

    private String photonBaseUrl = "https://photon.komoot.io";
}
