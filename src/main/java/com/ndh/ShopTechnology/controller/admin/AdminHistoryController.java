package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.PaginationMetadata;
import com.ndh.ShopTechnology.dto.response.log.AdminActivityLogResponse;
import com.ndh.ShopTechnology.dto.response.log.OrderHistoryResponse;
import com.ndh.ShopTechnology.dto.response.log.PriceEventHistoryResponse;
import com.ndh.ShopTechnology.dto.response.log.UnifiedHistoryResponse;
import com.ndh.ShopTechnology.services.log.AdminActivityLogService;
import com.ndh.ShopTechnology.services.log.OrderHistoryService;
import com.ndh.ShopTechnology.services.log.PriceEventHistoryService;
import com.ndh.ShopTechnology.services.log.UnifiedHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoints đọc lịch sử hệ thống (read-only).
 *
 * <pre>
 * ── Unified ──────────────────────────────────────────────────────────────────
 * GET /admin/history                         Tổng quát – gộp tất cả nguồn
 *
 * ── Order history ─────────────────────────────────────────────────────────────
 * GET /admin/history/orders/{orderId}        Lịch sử đầy đủ một đơn hàng
 * GET /admin/history/orders/{orderId}/status Chỉ thay đổi trạng thái đơn hàng
 * GET /admin/history/orders/{id}/return-refund  Chỉ lịch sử trả / hoàn tiền
 *
 * ── Admin activity log ────────────────────────────────────────────────────────
 * GET /admin/history/activity                Log admin (có lọc)
 * GET /admin/history/activity/entity/{t}/{id} Log của một entity cụ thể
 *
 * ── Price event history ───────────────────────────────────────────────────────
 * GET /admin/history/price-events            Lịch sử sự kiện chương trình giá
 * </pre>
 */
@RestController
@RequestMapping("${api.prefix}/admin/history")
@RequiredArgsConstructor
public class AdminHistoryController {

    private final OrderHistoryService      orderHistoryService;
    private final AdminActivityLogService  adminActivityLogService;
    private final UnifiedHistoryService    unifiedHistoryService;
    private final PriceEventHistoryService priceEventHistoryService;

    // ═══════════════════════════════════════════════════════════════════════════
    //  UNIFIED  –  GET /admin/history
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * API tổng quát – tìm kiếm lịch sử hệ thống gộp từ cả hai nguồn.
     *
     * <h3>Query params</h3>
     * <table>
     *   <tr><td>{@code source}</td>
     *       <td>Nguồn dữ liệu: {@code ALL} (default) | {@code ORDER_HISTORY} | {@code ACTIVITY_LOG}</td></tr>
     *   <tr><td>{@code entityType}</td>
     *       <td>Loại entity: {@code ORDER} | {@code PRODUCT} | {@code BRAND} |
     *           {@code CATEGORY} | {@code PRICE_CHANGE} | {@code VOLUME_TIER} | {@code PWP_OFFER}</td></tr>
     *   <tr><td>{@code entityId}</td>
     *       <td>ID của entity cụ thể (orderId nếu entityType=ORDER)</td></tr>
     *   <tr><td>{@code actorUserId}</td>
     *       <td>ID người thực hiện thao tác</td></tr>
     *   <tr><td>{@code actorRoleCode}</td>
     *       <td>Lọc theo role của người thực hiện: {@code EMPLOYEE} | {@code MANAGER} |
     *           {@code ADMIN} | {@code SUPER_ADMIN} (không phân biệt hoa thường)</td></tr>
     *   <tr><td>{@code action}</td>
     *       <td>Hành động: {@code CREATE} | {@code UPDATE} | {@code DELETE} |
     *           {@code ORDER_STATUS} | {@code RETURN_REFUND_STATUS}</td></tr>
     *   <tr><td>{@code from}</td>
     *       <td>Lọc từ ngày (ISO-8601, vd: {@code 2024-01-01T00:00:00})</td></tr>
     *   <tr><td>{@code to}</td>
     *       <td>Lọc đến ngày (ISO-8601)</td></tr>
     *   <tr><td>{@code page}</td><td>Trang, bắt đầu từ 0 (default: 0)</td></tr>
     *   <tr><td>{@code size}</td><td>Số bản ghi mỗi trang, tối đa 100 (default: 20)</td></tr>
     * </table>
     *
     * <h3>Ví dụ</h3>
     * <pre>
     * # Tất cả lịch sử của product id=5
     * GET /admin/history?entityType=PRODUCT&entityId=5
     *
     * # Tất cả thao tác CREATE của user id=3
     * GET /admin/history?actorUserId=3&action=CREATE
     *
     * # Chỉ lịch sử đơn hàng (order history + order activity)
     * GET /admin/history?source=ORDER_HISTORY
     *
     * # Lọc theo khoảng thời gian
     * GET /admin/history?from=2024-06-01T00:00:00&to=2024-06-30T23:59:59&size=50
     *
     * # Lịch sử thay đổi trạng thái của đơn hàng id=12
     * GET /admin/history?entityType=ORDER&entityId=12&action=ORDER_STATUS
     *
     * # Chỉ lịch sử do nhân viên (EMPLOYEE) thực hiện
     * GET /admin/history?actorRoleCode=EMPLOYEE
     *
     * # Nhân viên cập nhật đơn hàng
     * GET /admin/history?entityType=ORDER&actorRoleCode=EMPLOYEE&action=ORDER_STATUS
     * </pre>
     */
    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<UnifiedHistoryResponse>>> searchUnified(
            @RequestParam(required = false)                                         String source,
            @RequestParam(required = false)                                         String entityType,
            @RequestParam(required = false)                                         Long   entityId,
            @RequestParam(required = false)                                         Long   actorUserId,
            @RequestParam(required = false)                                         String actorRoleCode,
            @RequestParam(required = false)                                         String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);   // service tự sort DESC createdAt

        Page<UnifiedHistoryResponse> resultPage = unifiedHistoryService.search(
                source, entityType, entityId, actorUserId, actorRoleCode, action, from, to, pageable);

        return ResponseEntity.ok(APIResponse.<List<UnifiedHistoryResponse>>builder()
                .success(true)
                .message("Lấy lịch sử thành công")
                .data(resultPage.getContent())
                .metadata(PaginationMetadata.fromPage(resultPage))
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ORDER HISTORY  –  GET /admin/history/orders/...
    // ═══════════════════════════════════════════════════════════════════════════

    /** Toàn bộ lịch sử (status + return/refund) của một đơn hàng. */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<OrderHistoryResponse>>> getOrderHistory(
            @PathVariable Long orderId) {
        List<OrderHistoryResponse> data = orderHistoryService.getHistory(orderId)
                .stream().map(OrderHistoryResponse::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy lịch sử đơn hàng thành công", data, null,
                Map.of("count", data.size())));
    }

    /** Chỉ lịch sử thay đổi trạng thái đơn hàng. */
    @GetMapping("/orders/{orderId}/status")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<OrderHistoryResponse>>> getOrderStatusHistory(
            @PathVariable Long orderId) {
        List<OrderHistoryResponse> data = orderHistoryService.getOrderStatusHistory(orderId)
                .stream().map(OrderHistoryResponse::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy lịch sử trạng thái đơn hàng thành công", data, null,
                Map.of("count", data.size())));
    }

    /** Chỉ lịch sử trả hàng / hoàn tiền của một đơn hàng. */
    @GetMapping("/orders/{orderId}/return-refund")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<OrderHistoryResponse>>> getReturnRefundHistory(
            @PathVariable Long orderId) {
        List<OrderHistoryResponse> data = orderHistoryService.getReturnRefundHistory(orderId)
                .stream().map(OrderHistoryResponse::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy lịch sử trả hàng / hoàn tiền thành công", data, null,
                Map.of("count", data.size())));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ACTIVITY LOG  –  GET /admin/history/activity/...
    // ═══════════════════════════════════════════════════════════════════════════

    /** Log hoạt động admin có lọc và phân trang. */
    @GetMapping("/activity")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<AdminActivityLogResponse>>> searchActivityLog(
            @RequestParam(required = false)                                         Long   actorUserId,
            @RequestParam(required = false)                                         String entityType,
            @RequestParam(required = false)                                         String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminActivityLogResponse> resultPage = adminActivityLogService
                .search(actorUserId, entityType, action, from, to, pageable)
                .map(AdminActivityLogResponse::fromEntity);

        return ResponseEntity.ok(APIResponse.<List<AdminActivityLogResponse>>builder()
                .success(true)
                .message("Lấy log hoạt động thành công")
                .data(resultPage.getContent())
                .metadata(PaginationMetadata.fromPage(resultPage))
                .build());
    }

    /** Toàn bộ lịch sử thao tác trên một entity cụ thể. */
    @GetMapping("/activity/entity/{entityType}/{entityId}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<AdminActivityLogResponse>>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long   entityId) {
        List<AdminActivityLogResponse> data = adminActivityLogService
                .getByEntity(entityType.toUpperCase(), entityId)
                .stream().map(AdminActivityLogResponse::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(APIResponse.of(
                true, "Lấy lịch sử entity thành công", data, null,
                Map.of("count", data.size())));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PRICE EVENT HISTORY  –  GET /admin/history/price-events
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tìm kiếm lịch sử sự kiện chương trình giá từ bảng {@code price_event_history}.
     *
     * <h3>Query params</h3>
     * <table>
     *   <tr><td>{@code programType}</td>
     *       <td>{@code PRICE_CHANGE} | {@code VOLUME_TIER} | {@code PWP_OFFER}</td></tr>
     *   <tr><td>{@code programId}</td>
     *       <td>ID chương trình cụ thể</td></tr>
     *   <tr><td>{@code eventType}</td>
     *       <td>{@code CREATED} | {@code UPDATED} | {@code DELETED} |
     *           {@code ENABLED} | {@code DISABLED} | {@code STARTED} | {@code ENDED} | {@code EXPIRED}</td></tr>
     *   <tr><td>{@code productId}</td>
     *       <td>Lọc theo sản phẩm</td></tr>
     *   <tr><td>{@code from} / {@code to}</td>
     *       <td>Khoảng thời gian (ISO-8601)</td></tr>
     *   <tr><td>{@code page} / {@code size}</td>
     *       <td>Phân trang (max size = 100)</td></tr>
     * </table>
     */
    @GetMapping("/price-events")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<PriceEventHistoryResponse>>> searchPriceEvents(
            @RequestParam(required = false)                                         String programType,
            @RequestParam(required = false)                                         Long   programId,
            @RequestParam(required = false)                                         String eventType,
            @RequestParam(required = false)                                         Long   productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PriceEventHistoryResponse> resultPage = priceEventHistoryService
                .search(programType, programId, eventType, productId, from, to, pageable)
                .map(PriceEventHistoryResponse::fromEntity);

        return ResponseEntity.ok(APIResponse.<List<PriceEventHistoryResponse>>builder()
                .success(true)
                .message("Lấy lịch sử giá thành công")
                .data(resultPage.getContent())
                .metadata(PaginationMetadata.fromPage(resultPage))
                .build());
    }
}
