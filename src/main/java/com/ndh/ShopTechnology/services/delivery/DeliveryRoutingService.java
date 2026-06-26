package com.ndh.ShopTechnology.services.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.config.DeliveryRoutingProperties;
import com.ndh.ShopTechnology.dto.delivery.GeocodedAddress;
import com.ndh.ShopTechnology.dto.delivery.RouteMetrics;
import com.ndh.ShopTechnology.dto.response.delivery.ShippingDistanceResponse;
import com.ndh.ShopTechnology.dto.response.delivery.ShippingStoreOptionResponse;
import com.ndh.ShopTechnology.entities.store.StoreEntity;
import com.ndh.ShopTechnology.entities.user.AddressType;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.StoreRepository;
import com.ndh.ShopTechnology.repository.UserAddressRepository;
import com.ndh.ShopTechnology.utils.ShippingFeeCalculator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DeliveryRoutingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DeliveryRoutingProperties properties;
    private final UserAddressRepository userAddressRepository;
    private final StoreRepository storeRepository;

    public DeliveryRoutingService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            DeliveryRoutingProperties properties,
            UserAddressRepository userAddressRepository,
            StoreRepository storeRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.userAddressRepository = userAddressRepository;
        this.storeRepository = storeRepository;
    }

    /**
     * Với một địa chỉ giao, trả về danh sách kho đang hoạt động kèm khoảng cách và
     * phí ship từ từng kho tới địa chỉ đó (để khách chọn store khi checkout).
     */
    public List<ShippingStoreOptionResponse> storeShippingOptions(String address) {
        GeocodedAddress origin = geocodeAddress(address);
        List<StoreEntity> stores = storeRepository.findByActiveTrueOrderByIdAsc();
        List<ShippingStoreOptionResponse> out = new ArrayList<>(stores.size());
        for (StoreEntity s : stores) {
            ShippingStoreOptionResponse.ShippingStoreOptionResponseBuilder b = ShippingStoreOptionResponse.builder()
                    .storeId(s.getId())
                    .code(s.getCode())
                    .name(s.getName())
                    .addressLine(s.getAddressLine())
                    .city(s.getCity())
                    .storeLatitude(s.getLatitude())
                    .storeLongitude(s.getLongitude());
            if (s.getLatitude() == null || s.getLongitude() == null) {
                out.add(b.routable(false).build());
                continue;
            }
            try {
                RouteMetrics r = routeDriving(s.getLatitude(), s.getLongitude(),
                        origin.latitude(), origin.longitude());
                out.add(b.routable(true)
                        .distanceMeters(r.distanceMeters())
                        .distanceKilometers(Math.round(r.distanceMeters() / 10.0) / 100.0)
                        .durationSeconds(r.durationSeconds())
                        .shippingFeeVnd(ShippingFeeCalculator.fromDistanceMeters(r.distanceMeters()))
                        .build());
            } catch (CustomApiException ex) {
                out.add(b.routable(false).build());
            }
        }
        return out;
    }

    /** Khoảng cách & phí ship từ một kho cụ thể tới địa chỉ giao. */
    public ShippingDistanceResponse distanceFromStoreToAddress(Long storeId, String address) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomApiException(HttpStatus.NOT_FOUND, "Không tìm thấy kho id=" + storeId));
        if (store.getLatitude() == null || store.getLongitude() == null) {
            throw new CustomApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Kho '" + store.getName() + "' chưa có toạ độ để tính phí ship.");
        }
        GeocodedAddress origin = geocodeAddress(address);
        RouteMetrics r = routeDriving(store.getLatitude(), store.getLongitude(),
                origin.latitude(), origin.longitude());
        return ShippingDistanceResponse.builder()
                .distanceMeters(r.distanceMeters())
                .distanceKilometers(Math.round(r.distanceMeters() / 10.0) / 100.0)
                .durationSeconds(r.durationSeconds())
                .resolvedAddress(origin.displayName())
                .originLatitude(origin.latitude())
                .originLongitude(origin.longitude())
                .warehouseLatitude(store.getLatitude())
                .warehouseLongitude(store.getLongitude())
                .shippingFeeVnd(ShippingFeeCalculator.fromDistanceMeters(r.distanceMeters()))
                .build();
    }

    public double[] resolveWarehouseLatLon() {
        return userAddressRepository
                .findFirstByAddressType(AddressType.WAREHOUSE)
                .filter(w -> w.getLatitude() != null && w.getLongitude() != null)
                .map(w -> new double[] { w.getLatitude(), w.getLongitude() })
                .orElseGet(() -> new double[] {
                    properties.getWarehouseLatitude(),
                    properties.getWarehouseLongitude()
                });
    }

    public GeocodedAddress geocodeAddress(String address) {
        String trimmed = address == null ? "" : address.trim();
        if (trimmed.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "address is required");
        }
        GeocodedPoint p = geocode(trimmed);
        return new GeocodedAddress(p.lat(), p.lon(), p.displayName());
    }

    public RouteMetrics routeDriving(double fromLat, double fromLon, double toLat, double toLon) {
        try {
            String path = String.format(
                    "/route/v1/driving/%s,%s;%s,%s",
                    formatCoord(fromLon),
                    formatCoord(fromLat),
                    formatCoord(toLon),
                    formatCoord(toLat));
            URI uri = UriComponentsBuilder
                    .fromUriString(properties.getOsrmBaseUrl())
                    .path(path)
                    .queryParam("overview", "false")
                    .queryParam("steps", "false")
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            String body = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(body);
            String code = root.path("code").asText("");
            if (!"Ok".equals(code)) {
                String msg = root.path("message").asText("No route found");
                throw new CustomApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "OSRM: " + (msg.isEmpty() ? code : msg));
            }
            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new CustomApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No route between coordinates");
            }
            JsonNode route0 = routes.get(0);
            return new RouteMetrics(route0.path("distance").asDouble(), route0.path("duration").asDouble());
        } catch (CustomApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw new CustomApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Routing service unavailable: " + e.getMessage());
        } catch (Exception e) {
            throw new CustomApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to parse routing response: " + e.getMessage());
        }
    }

    public ShippingDistanceResponse distanceFromAddressToWarehouse(String address) {
        GeocodedAddress origin = geocodeAddress(address);
        double[] wh = resolveWarehouseLatLon();
        double wLat = wh[0];
        double wLon = wh[1];
        RouteMetrics r = routeDriving(origin.latitude(), origin.longitude(), wLat, wLon);
        return ShippingDistanceResponse.builder()
                .distanceMeters(r.distanceMeters())
                .distanceKilometers(Math.round(r.distanceMeters() / 10.0) / 100.0)
                .durationSeconds(r.durationSeconds())
                .resolvedAddress(origin.displayName())
                .originLatitude(origin.latitude())
                .originLongitude(origin.longitude())
                .warehouseLatitude(wLat)
                .warehouseLongitude(wLon)
                .shippingFeeVnd(ShippingFeeCalculator.fromDistanceMeters(r.distanceMeters()))
                .build();
    }

    private record GeocodedPoint(double lat, double lon, String displayName) {}

    private GeocodedPoint geocode(String address) {
        String q = enrichQueryForGeocode(address);
        GeocodedPoint nominatimResult = null;
        try {
            nominatimResult = nominatimSearch(q, true);
            if (nominatimResult == null) {
                nominatimResult = nominatimSearch(q, false);
            }
        } catch (HttpClientErrorException e) {
            if (properties.isPhotonFallbackEnabled() && shouldFallbackFromNominatim(e.getStatusCode())) {
                nominatimResult = null;
            } else {
                throw new CustomApiException(
                        HttpStatus.BAD_GATEWAY,
                        "Geocoding (Nominatim): " + e.getStatusCode().value() + " " + e.getStatusText());
            }
        } catch (RestClientException e) {
            if (!properties.isPhotonFallbackEnabled()) {
                throw new CustomApiException(
                        HttpStatus.BAD_GATEWAY,
                        "Geocoding service unavailable: " + e.getMessage());
            }
            nominatimResult = null;
        }
        if (nominatimResult != null) {
            return nominatimResult;
        }
        if (properties.isPhotonFallbackEnabled()) {
            GeocodedPoint p = photonSearch(q, false);
            if (p != null) {
                return p;
            }
            p = photonSearch(q, true);
            if (p != null) {
                return p;
            }
        }
        throw new CustomApiException(
                HttpStatus.BAD_REQUEST,
                "Could not geocode address; try a more specific address");
    }

    private static String enrichQueryForGeocode(String address) {
        String t = address.trim();
        if (t.isEmpty()) {
            return t;
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.contains("vietnam")
                || lower.contains("việt nam")
                || lower.contains("viet nam")
                || lower.endsWith(", vn")
                || lower.contains(", vn,")) {
            return t;
        }
        return t + ", Vietnam";
    }

    private static boolean shouldFallbackFromNominatim(HttpStatusCode status) {
        int code = status.value();
        return code == HttpStatus.FORBIDDEN.value()
                || code == HttpStatus.TOO_MANY_REQUESTS.value()
                || code == HttpStatus.SERVICE_UNAVAILABLE.value()
                || code == HttpStatus.BAD_GATEWAY.value();
    }

    private GeocodedPoint nominatimSearch(String address, boolean useCountryFilter) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromUriString(properties.getNominatimBaseUrl())
                .path("/search")
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("limit", 5)
                .queryParam("addressdetails", "1");
        if (useCountryFilter && StringUtils.isNotBlank(properties.getCountryCodes())) {
            b.queryParam("countrycodes", properties.getCountryCodes());
        }
        URI uri = b.encode(StandardCharsets.UTF_8).build().toUri();
        HttpHeaders headers = nominatimHeaders();
        ResponseEntity<String> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return nominatimFirstFromBody(response.getBody());
    }

    private HttpHeaders nominatimHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String email = StringUtils.trimToEmpty(properties.getNominatimContactEmail());
        String ua = properties.getHttpUserAgent();
        if (StringUtils.isNotBlank(email)) {
            headers.add("From", email);
            if (!ua.contains("@")) {
                ua = ua + " (contact: " + email + ")";
            }
        }
        headers.add(HttpHeaders.USER_AGENT, ua);
        headers.add(HttpHeaders.ACCEPT_LANGUAGE, "vi,en");
        return headers;
    }

    private GeocodedPoint nominatimFirstFromBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode arr;
        try {
            arr = objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
        if (!arr.isArray() || arr.isEmpty()) {
            return null;
        }
        for (JsonNode node : arr) {
            JsonNode latN = node.get("lat");
            JsonNode lonN = node.get("lon");
            if (latN == null || lonN == null || latN.isNull() || lonN.isNull()) {
                continue;
            }
            double lat = latN.asDouble();
            double lon = lonN.asDouble();
            String display = node.path("display_name").asText(null);
            return new GeocodedPoint(lat, lon, display);
        }
        return null;
    }

    private GeocodedPoint photonSearch(String address, boolean biasToWarehouse) {
        try {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromUriString(properties.getPhotonBaseUrl())
                    .path("/api")
                    .queryParam("q", address)
                    .queryParam("lang", "vi")
                    .queryParam("limit", 5);
            if (biasToWarehouse) {
                b.queryParam("lat", properties.getWarehouseLatitude())
                        .queryParam("lon", properties.getWarehouseLongitude());
            }
            URI uri = b.encode(StandardCharsets.UTF_8).build().toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.USER_AGENT, properties.getHttpUserAgent());
            headers.add(HttpHeaders.ACCEPT_LANGUAGE, "vi,en");

            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return photonFirstFromBody(response.getBody());
        } catch (RestClientException e) {
            return null;
        }
    }

    private GeocodedPoint photonFirstFromBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                return null;
            }
            for (JsonNode feat : features) {
                JsonNode coords = feat.path("geometry").path("coordinates");
                if (!coords.isArray() || coords.size() < 2) {
                    continue;
                }
                double lon = coords.get(0).asDouble();
                double lat = coords.get(1).asDouble();
                String display = buildPhotonDisplayName(feat.path("properties"));
                return new GeocodedPoint(lat, lon, display);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildPhotonDisplayName(JsonNode props) {
        if (props == null || props.isMissingNode()) {
            return null;
        }
        String name = props.path("name").asText("");
        String street = props.path("street").asText("");
        String city = props.path("city").asText("");
        String country = props.path("country").asText("");
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(name)) {
            sb.append(name);
        }
        if (StringUtils.isNotBlank(street) && !street.equals(name)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(street);
        }
        if (StringUtils.isNotBlank(city)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(city);
        }
        if (StringUtils.isNotBlank(country)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(country);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static String formatCoord(double value) {
        return String.format(java.util.Locale.US, "%.6f", value);
    }
}
