package com.ndh.ShopTechnology.services.report;

import com.ndh.ShopTechnology.dto.response.report.TopProductReportDto;

import java.util.List;

public interface ReportService {
    List<TopProductReportDto> getTopSellingProducts(String timeRange, int limit);
    List<TopProductReportDto> getTopInterestedProducts(String timeRange, int limit);
}
