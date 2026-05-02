package com.ndh.ShopTechnology.dto.delivery;

/**
 * Kết quả geocoding (Nominatim/OSM).
 */
public record GeocodedAddress(double latitude, double longitude, String displayName) {}
