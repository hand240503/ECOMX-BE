package com.ndh.ShopTechnology.controller.log;

import com.ndh.ShopTechnology.dto.request.log.CreateCollectorLogRequest;
import com.ndh.ShopTechnology.dto.request.log.FilterCollectorLogRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.dto.response.log.CollectorLogResponse;
import com.ndh.ShopTechnology.services.log.CollectorLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/collector-logs")
public class CollectorLogController {

    private final CollectorLogService collectorLogService;

    @Autowired
    public CollectorLogController(CollectorLogService collectorLogService) {
        this.collectorLogService = collectorLogService;
    }

    @PostMapping
    public ResponseEntity<APIResponse<CollectorLogResponse>> createLog(
            @Valid @RequestBody CreateCollectorLogRequest request) {
        try {
            CollectorLogResponse log = collectorLogService.createLog(request);
            APIResponse<CollectorLogResponse> response = APIResponse.of(
                    true,
                    "Collector log created successfully",
                    log,
                    null,
                    null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            APIResponse<CollectorLogResponse> response = APIResponse.of(
                    false,
                    "Failed to create collector log: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("log")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<CollectorLogResponse>>> getAllLogs() {
        try {
            List<CollectorLogResponse> logs = collectorLogService.getAllLogs();
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    true,
                    "Collector logs retrieved successfully",
                    logs,
                    null,
                    Map.of("count", logs.size()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve collector logs: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("logs")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<CollectorLogResponse>> getLogById(@PathVariable Long id) {
        try {
            CollectorLogResponse log = collectorLogService.getLogById(id);
            APIResponse<CollectorLogResponse> response = APIResponse.of(
                    true,
                    "Collector log retrieved successfully",
                    log,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<CollectorLogResponse> response = APIResponse.of(
                    false,
                    "Collector log not found",
                    null,
                    List.of(ErrorResponse.builder()
                            .field("id")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<APIResponse<List<CollectorLogResponse>>> getLogsByUserId(@PathVariable Long userId) {
        try {
            List<CollectorLogResponse> logs = collectorLogService.getLogsByUserId(userId);
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    true,
                    "Collector logs retrieved successfully",
                    logs,
                    null,
                    Map.of("count", logs.size(), "userId", userId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve collector logs: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("userId")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<APIResponse<List<CollectorLogResponse>>> getLogsByProductId(@PathVariable Long productId) {
        try {
            List<CollectorLogResponse> logs = collectorLogService.getLogsByProductId(productId);
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    true,
                    "Collector logs retrieved successfully",
                    logs,
                    null,
                    Map.of("count", logs.size(), "productId", productId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve collector logs: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("productId")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/event/{event}")
    public ResponseEntity<APIResponse<List<CollectorLogResponse>>> getLogsByEvent(@PathVariable String event) {
        try {
            List<CollectorLogResponse> logs = collectorLogService.getLogsByEvent(event);
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    true,
                    "Collector logs retrieved successfully",
                    logs,
                    null,
                    Map.of("count", logs.size(), "event", event));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve collector logs: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("event")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<APIResponse<List<CollectorLogResponse>>> getLogsBySessionId(@PathVariable String sessionId) {
        try {
            List<CollectorLogResponse> logs = collectorLogService.getLogsBySessionId(sessionId);
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    true,
                    "Collector logs retrieved successfully",
                    logs,
                    null,
                    Map.of("count", logs.size(), "sessionId", sessionId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve collector logs: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("sessionId")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<APIResponse<List<CollectorLogResponse>>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        try {
            List<CollectorLogResponse> logs = collectorLogService.getLogsByDateRange(startDate, endDate);
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    true,
                    "Collector logs retrieved successfully",
                    logs,
                    null,
                    Map.of("count", logs.size(), "startDate", startDate, "endDate", endDate));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    false,
                    "Failed to retrieve collector logs: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("dateRange")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/filter")
    public ResponseEntity<APIResponse<List<CollectorLogResponse>>> filterLogs(
            @RequestBody FilterCollectorLogRequest request) {
        try {
            List<CollectorLogResponse> logs = collectorLogService.filterLogs(request);
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    true,
                    "Collector logs filtered successfully",
                    logs,
                    null,
                    Map.of("count", logs.size()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<List<CollectorLogResponse>> response = APIResponse.of(
                    false,
                    "Failed to filter collector logs: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("filter")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteLog(@PathVariable Long id) {
        try {
            collectorLogService.deleteLog(id);
            APIResponse<Void> response = APIResponse.of(
                    true,
                    "Collector log deleted successfully",
                    null,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            APIResponse<Void> response = APIResponse.of(
                    false,
                    "Failed to delete collector log: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("id")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
