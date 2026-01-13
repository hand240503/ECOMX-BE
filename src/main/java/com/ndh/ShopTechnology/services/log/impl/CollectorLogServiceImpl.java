package com.ndh.ShopTechnology.services.log.impl;

import com.ndh.ShopTechnology.dto.request.log.CreateCollectorLogRequest;
import com.ndh.ShopTechnology.dto.request.log.FilterCollectorLogRequest;
import com.ndh.ShopTechnology.dto.response.log.CollectorLogResponse;
import com.ndh.ShopTechnology.entities.log.CollectorLogEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.CollectorLogRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.log.CollectorLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollectorLogServiceImpl implements CollectorLogService {

    private final CollectorLogRepository collectorLogRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CollectorLogServiceImpl(CollectorLogRepository collectorLogRepository,
                                   UserRepository userRepository,
                                   ProductRepository productRepository) {
        this.collectorLogRepository = collectorLogRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CollectorLogResponse createLog(CreateCollectorLogRequest request) {
        CollectorLogEntity.CollectorLogEntityBuilder builder = CollectorLogEntity.builder()
                .event(request.getEvent())
                .sessionId(request.getSessionId())
                .deviceType(request.getDeviceType())
                .platform(request.getPlatform())
                .metadata(request.getMetadata())
                .ipAddress(request.getIpAddress())
                .timestamp(request.getTimestamp() != null ? request.getTimestamp() : new Date());

        // Set user if provided
        if (request.getUserId() != null) {
            UserEntity user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new NotFoundEntityException("User not found with id: " + request.getUserId()));
            builder.user(user);
        }

        // Set product if provided
        if (request.getProductId() != null) {
            ProductEntity product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + request.getProductId()));
            builder.product(product);
        }

        CollectorLogEntity log = collectorLogRepository.save(builder.build());
        return CollectorLogResponse.fromEntity(log);
    }

    @Override
    public CollectorLogResponse getLogById(Long id) {
        CollectorLogEntity log = collectorLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Collector log not found with id: " + id));
        return CollectorLogResponse.fromEntity(log);
    }

    @Override
    public List<CollectorLogResponse> getAllLogs() {
        List<CollectorLogEntity> logs = collectorLogRepository.findAll();
        return logs.stream()
                .map(CollectorLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CollectorLogResponse> getLogsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundEntityException("User not found with id: " + userId);
        }
        List<CollectorLogEntity> logs = collectorLogRepository.findByUserId(userId);
        return logs.stream()
                .map(CollectorLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CollectorLogResponse> getLogsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundEntityException("Product not found with id: " + productId);
        }
        List<CollectorLogEntity> logs = collectorLogRepository.findByProductId(productId);
        return logs.stream()
                .map(CollectorLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CollectorLogResponse> getLogsByEvent(String event) {
        List<CollectorLogEntity> logs = collectorLogRepository.findByEvent(event);
        return logs.stream()
                .map(CollectorLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CollectorLogResponse> getLogsBySessionId(String sessionId) {
        List<CollectorLogEntity> logs = collectorLogRepository.findBySessionId(sessionId);
        return logs.stream()
                .map(CollectorLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CollectorLogResponse> getLogsByDateRange(Date startDate, Date endDate) {
        List<CollectorLogEntity> logs = collectorLogRepository.findByTimestampBetween(startDate, endDate);
        return logs.stream()
                .map(CollectorLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CollectorLogResponse> filterLogs(FilterCollectorLogRequest request) {
        List<CollectorLogEntity> logs;

        // Build query based on filters
        if (request.getUserId() != null && request.getStartDate() != null && request.getEndDate() != null) {
            logs = collectorLogRepository.findByUserIdAndTimestampBetween(
                    request.getUserId(), request.getStartDate(), request.getEndDate());
        } else if (request.getProductId() != null && request.getStartDate() != null && request.getEndDate() != null) {
            logs = collectorLogRepository.findByProductIdAndTimestampBetween(
                    request.getProductId(), request.getStartDate(), request.getEndDate());
        } else if (request.getUserId() != null) {
            logs = collectorLogRepository.findByUserId(request.getUserId());
        } else if (request.getProductId() != null) {
            logs = collectorLogRepository.findByProductId(request.getProductId());
        } else if (request.getEvent() != null) {
            logs = collectorLogRepository.findByEvent(request.getEvent());
        } else if (request.getSessionId() != null) {
            logs = collectorLogRepository.findBySessionId(request.getSessionId());
        } else if (request.getStartDate() != null && request.getEndDate() != null) {
            logs = collectorLogRepository.findByTimestampBetween(request.getStartDate(), request.getEndDate());
        } else {
            logs = collectorLogRepository.findAll();
        }

        // Apply pagination if provided
        if (request.getPage() != null && request.getSize() != null) {
            int page = request.getPage() > 0 ? request.getPage() - 1 : 0;
            int size = request.getSize() > 0 ? request.getSize() : 10;
            int start = page * size;
            int end = Math.min(start + size, logs.size());
            if (start < logs.size()) {
                logs = logs.subList(start, end);
            } else {
                logs = List.of();
            }
        }

        return logs.stream()
                .map(CollectorLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteLog(Long id) {
        CollectorLogEntity log = collectorLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Collector log not found with id: " + id));
        collectorLogRepository.delete(log);
    }
}
