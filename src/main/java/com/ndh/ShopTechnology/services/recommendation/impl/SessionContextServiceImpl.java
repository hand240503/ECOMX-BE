package com.ndh.ShopTechnology.services.recommendation.impl;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.repository.CollectorLogRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.recommendation.SessionContextService;
import com.ndh.ShopTechnology.services.recommendation.dto.SessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class SessionContextServiceImpl implements SessionContextService {

    private final CollectorLogRepository collectorLogRepository;
    private final ProductRepository productRepository;

    @Value("${recommendation.session.window-minutes:10}")
    private int sessionWindowMinutes;

    @Value("${recommendation.session.max-recent-products:10}")
    private int maxRecentProducts;

    public SessionContextServiceImpl(
            CollectorLogRepository collectorLogRepository,
            ProductRepository productRepository) {
        this.collectorLogRepository = collectorLogRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SessionContext buildContext(Long userId) {
        if (userId == null) return emptyContext(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusMinutes(sessionWindowMinutes);

        List<Object[]> rows = collectorLogRepository
                .findRecentProductActivityByUser(userId, since, maxRecentProducts);

        log.info("[Session] userId={} found {} recent products in last {} min",
                userId, rows.size(), sessionWindowMinutes);

        if (rows.isEmpty()) return emptyContext(userId);

        // Tính weight cho từng product theo recency + frequency
        Map<Long, Double> productWeights = new HashMap<>();
        for (Object[] row : rows) {
            Long pid = ((Number) row[0]).longValue();
            LocalDateTime lastTs = ((Timestamp) row[1]).toLocalDateTime();
            long cnt = ((Number) row[2]).longValue();

            long minutesAgo = Math.max(0, ChronoUnit.MINUTES.between(lastTs, now));

            double recency = 1.0 / (1.0 + minutesAgo);   // càng mới càng cao
            double freq    = Math.log(1.0 + cnt);         // càng nhiều càng cao
            double w       = recency * (1.0 + freq);

            productWeights.put(pid, w);
        }

        // Aggregate weight theo category & brand
        List<ProductEntity> products = productRepository.findAllById(productWeights.keySet());

        Map<Long, Double> categoryWeights = new HashMap<>();
        Map<String, Double> brandWeights = new HashMap<>();

        for (ProductEntity p : products) {
            Double w = productWeights.get(p.getId());
            if (w == null) continue;

            if (p.getCategory() != null) {
                categoryWeights.merge(p.getCategory().getId(), w, Double::sum);
            }
            if (p.getBrand() != null) {
                brandWeights.merge(p.getBrand().getName(), w, Double::sum);
            }
        }

        log.info("[Session] userId={} categoryWeights={} brandWeights={}",
                userId, categoryWeights, brandWeights);

        return SessionContext.builder()
                .userId(userId)
                .sessionProductIds(new HashSet<>(productWeights.keySet()))
                .categoryWeights(categoryWeights)
                .brandWeights(brandWeights)
                .build();
    }

    private SessionContext emptyContext(Long userId) {
        return SessionContext.builder()
                .userId(userId)
                .sessionProductIds(Set.of())
                .categoryWeights(Map.of())
                .brandWeights(Map.of())
                .build();
    }
}