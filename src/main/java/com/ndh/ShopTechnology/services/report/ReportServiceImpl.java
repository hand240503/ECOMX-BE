package com.ndh.ShopTechnology.services.report;

import com.ndh.ShopTechnology.dto.response.report.TopProductReportDto;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.repository.OrderDetailRepository;
import com.ndh.ShopTechnology.repository.PopularityRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.recommendation.dto.PopularProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderDetailRepository orderDetailRepository;
    private final PopularityRepository popularityRepository;
    private final ProductRepository productRepository;

    @Override
    public List<TopProductReportDto> getTopSellingProducts(String timeRange, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Date since = getSinceDate(timeRange);

        List<Object[]> results;
        if (since == null) {
            // all time: we can use product's soldCount or query all time orders.
            // Using query all time with since = a very old date (e.g., 2000-01-01) for consistency
            Calendar cal = Calendar.getInstance();
            cal.set(2000, Calendar.JANUARY, 1);
            since = cal.getTime();
        }
        
        results = orderDetailRepository.findTopSellingSince(since, pageable);

        return mapToReportDtoFromObjectArray(results);
    }

    @Override
    public List<TopProductReportDto> getTopInterestedProducts(String timeRange, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Date since = getSinceDate(timeRange);

        List<PopularProductRow> results;
        if (since == null) {
            results = popularityRepository.findTopPopular(pageable);
        } else {
            results = popularityRepository.findTopTrendingSince(since, pageable);
        }

        return mapToReportDtoFromPopularRows(results);
    }

    private Date getSinceDate(String timeRange) {
        if ("all".equalsIgnoreCase(timeRange)) {
            return null;
        }
        
        Calendar cal = Calendar.getInstance();
        if ("week".equalsIgnoreCase(timeRange)) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
        } else if ("month".equalsIgnoreCase(timeRange)) {
            cal.add(Calendar.DAY_OF_YEAR, -30);
        } else if ("year".equalsIgnoreCase(timeRange)) {
            cal.add(Calendar.DAY_OF_YEAR, -365);
        } else {
            return null;
        }
        return cal.getTime();
    }

    private List<TopProductReportDto> mapToReportDtoFromObjectArray(List<Object[]> rawData) {
        if (rawData == null || rawData.isEmpty()) return Collections.emptyList();

        List<Long> productIds = rawData.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toList());

        Map<Long, ProductEntity> productMap = getProductMap(productIds);

        return rawData.stream().map(row -> {
            Long productId = ((Number) row[0]).longValue();
            Long cnt = ((Number) row[1]).longValue();
            ProductEntity product = productMap.get(productId);
            
            return TopProductReportDto.builder()
                    .productId(productId)
                    .productName(product != null ? product.getProductName() : "Unknown")
                    .sku(product != null && product.getSku() != null ? String.valueOf(product.getSku()) : null)
                    .count(cnt)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<TopProductReportDto> mapToReportDtoFromPopularRows(List<PopularProductRow> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        List<Long> productIds = rows.stream()
                .map(PopularProductRow::getProductId)
                .collect(Collectors.toList());

        Map<Long, ProductEntity> productMap = getProductMap(productIds);

        return rows.stream().map(row -> {
            Long productId = row.getProductId();
            ProductEntity product = productMap.get(productId);
            
            return TopProductReportDto.builder()
                    .productId(productId)
                    .productName(product != null ? product.getProductName() : "Unknown")
                    .sku(product != null && product.getSku() != null ? String.valueOf(product.getSku()) : null)
                    .count(row.getCnt())
                    .build();
        }).collect(Collectors.toList());
    }

    private Map<Long, ProductEntity> getProductMap(List<Long> productIds) {
        List<ProductEntity> products = productRepository.findAllById(productIds);
        return products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, p -> p, (p1, p2) -> p1));
    }
}
