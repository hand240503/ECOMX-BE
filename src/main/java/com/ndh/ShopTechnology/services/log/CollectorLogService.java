package com.ndh.ShopTechnology.services.log;

import com.ndh.ShopTechnology.dto.request.log.CreateCollectorLogRequest;
import com.ndh.ShopTechnology.dto.request.log.FilterCollectorLogRequest;
import com.ndh.ShopTechnology.dto.response.log.CollectorLogResponse;

import java.util.Date;
import java.util.List;

public interface CollectorLogService {
    CollectorLogResponse createLog(CreateCollectorLogRequest request);
    CollectorLogResponse getLogById(Long id);
    List<CollectorLogResponse> getAllLogs();
    List<CollectorLogResponse> getLogsByUserId(Long userId);
    List<CollectorLogResponse> getLogsByProductId(Long productId);
    List<CollectorLogResponse> getLogsByEvent(String event);
    List<CollectorLogResponse> getLogsBySessionId(String sessionId);
    List<CollectorLogResponse> getLogsByDateRange(Date startDate, Date endDate);
    List<CollectorLogResponse> filterLogs(FilterCollectorLogRequest request);
    void deleteLog(Long id);
}
