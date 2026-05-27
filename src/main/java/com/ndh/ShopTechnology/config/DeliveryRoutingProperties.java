package com.ndh.ShopTechnology.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "delivery.routing")
public class DeliveryRoutingProperties {

    private double warehouseLatitude = 15.975435631036287;

    private double warehouseLongitude = 108.25329136862204;

    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org";

    private String osrmBaseUrl = "https://router.project-osrm.org";

    private String countryCodes = "vn";

    private String nominatimContactEmail = "";

    private String httpUserAgent = "ShopTechnology/1.0 (ecomx-be)";

    private boolean photonFallbackEnabled = true;

    private String photonBaseUrl = "https://photon.komoot.io";
}
