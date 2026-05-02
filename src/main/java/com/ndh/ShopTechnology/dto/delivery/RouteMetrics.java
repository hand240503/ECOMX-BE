package com.ndh.ShopTechnology.dto.delivery;

/** Kết quả rút gọn từ OSRM (driving). */
public record RouteMetrics(double distanceMeters, double durationSeconds) {}
