package com.ndh.ShopTechnology.services.log.impl;

import com.ndh.ShopTechnology.dto.response.log.UnifiedHistoryResponse;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import com.ndh.ShopTechnology.entities.log.OrderHistoryEntity;
import com.ndh.ShopTechnology.repository.AdminActivityLogRepository;
import com.ndh.ShopTechnology.repository.OrderHistoryRepository;
import com.ndh.ShopTechnology.services.log.UnifiedHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Gộp kết quả từ {@code order_history} và {@code admin_activity_log} thành
 * một danh sách thống nhất rồi phân trang trong bộ nhớ.
 *
 * <p><b>Chiến lược fetch:</b>
 * <ul>
 *   <li>Với mỗi nguồn cần truy vấn, fetch tối đa {@link #MAX_FETCH_PER_SOURCE} bản ghi
 *       (đã áp dụng đầy đủ filter, sắp xếp DESC theo {@code createdAt}).</li>
 *   <li>Gộp hai danh sách, sắp xếp lại, rồi cắt trang.</li>
 * </ul>
 *
 * <p>Đây là cách tiếp cận thực dụng phù hợp với dữ liệu admin vừa phải.
 * Nếu dữ liệu rất lớn (> hàng triệu bản ghi), cần chuyển sang native UNION query.
 */
@Service
@RequiredArgsConstructor
public class UnifiedHistoryServiceImpl implements UnifiedHistoryService {

    /** Số bản ghi tối đa fetch từ mỗi nguồn trước khi gộp/phân trang */
    private static final int MAX_FETCH_PER_SOURCE = 2_000;

    // Hằng nguồn dữ liệu
    public static final String SOURCE_ALL           = "ALL";
    public static final String SOURCE_ORDER_HISTORY = "ORDER_HISTORY";
    public static final String SOURCE_ACTIVITY_LOG  = "ACTIVITY_LOG";

    // Hằng action đặc thù của order_history
    private static final Set<String> ORDER_ACTIONS = Set.of(
            "ORDER_STATUS", "RETURN_REFUND_STATUS");

    private final OrderHistoryRepository     orderHistoryRepository;
    private final AdminActivityLogRepository adminActivityLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UnifiedHistoryResponse> search(
            String   source,
            String   entityType,
            Long     entityId,
            Long     actorUserId,
            String   actorRoleCode,
            String   action,
            Date     from,
            Date     to,
            Pageable pageable) {

        String normalizedSource     = normalizeSource(source);
        String normalizedEntityType = entityType     != null ? entityType.toUpperCase()     : null;
        String normalizedAction     = action         != null ? action.toUpperCase()         : null;
        String normalizedRoleCode   = actorRoleCode  != null ? actorRoleCode.toUpperCase()  : null;

        // ── Xác định cần query nguồn nào ──────────────────────────────────────
        boolean needOrderHistory = needOrderHistory(
                normalizedSource, normalizedEntityType, normalizedAction);
        boolean needActivityLog  = needActivityLog(
                normalizedSource, normalizedEntityType, normalizedAction);

        List<UnifiedHistoryResponse> merged = new ArrayList<>();

        // ── Query order_history ───────────────────────────────────────────────
        if (needOrderHistory) {
            // entityId trong order_history là orderId
            Long orderId        = "ORDER".equals(normalizedEntityType) ? entityId : null;
            // nếu không phải ORDER type nhưng source = ALL và entityId có giá trị → bỏ qua
            boolean skipOrderHistory = needActivityLog
                    && normalizedEntityType != null
                    && !"ORDER".equals(normalizedEntityType);
            if (!skipOrderHistory) {
                String changeType = toChangeType(normalizedAction);
                orderHistoryRepository
                        .search(orderId, actorUserId, changeType, from, to, normalizedRoleCode)
                        .stream()
                        .limit(MAX_FETCH_PER_SOURCE)
                        .map(UnifiedHistoryResponse::fromOrderHistory)
                        .forEach(merged::add);
            }
        }

        // ── Query admin_activity_log ──────────────────────────────────────────
        if (needActivityLog) {
            boolean skipActivityLog = needOrderHistory
                    && "ORDER".equals(normalizedEntityType);
            if (!skipActivityLog) {
                String actLogAction     = toActivityLogAction(normalizedAction);
                String actLogEntityType = "ORDER".equals(normalizedEntityType) ? null : normalizedEntityType;

                // fetch thủ công (không dùng Pageable để sau đó merge)
                Pageable bigPage = PageRequest.of(0, MAX_FETCH_PER_SOURCE);
                adminActivityLogRepository
                        .search(actorUserId, actLogEntityType, actLogAction, from, to, normalizedRoleCode, bigPage)
                        .getContent()
                        .stream()
                        // lọc thêm entityId nếu có
                        .filter(e -> entityId == null || entityId.equals(e.getEntityId()))
                        .map(UnifiedHistoryResponse::fromActivityLog)
                        .forEach(merged::add);
            }
        }

        // ── Sắp xếp tổng hợp theo createdAt DESC ─────────────────────────────
        merged.sort(Comparator.comparing(
                UnifiedHistoryResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // ── Phân trang thủ công ───────────────────────────────────────────────
        int total     = merged.size();
        int pageNum   = pageable.getPageNumber();
        int pageSize  = pageable.getPageSize();
        int fromIdx   = Math.min(pageNum * pageSize, total);
        int toIdx     = Math.min(fromIdx + pageSize, total);
        List<UnifiedHistoryResponse> pageContent = merged.subList(fromIdx, toIdx);

        return new PageImpl<>(pageContent, pageable, total);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String normalizeSource(String source) {
        if (source == null || source.isBlank()) return SOURCE_ALL;
        return source.toUpperCase();
    }

    /** Cần query order_history không? */
    private static boolean needOrderHistory(String source, String entityType, String action) {
        if (SOURCE_ACTIVITY_LOG.equals(source)) return false;
        // nếu entityType rõ ràng là non-ORDER → không cần order_history
        if (entityType != null && !entityType.equals("ORDER")) return false;
        // nếu action rõ ràng là activity-log action → không cần order_history
        if (action != null && !ORDER_ACTIONS.contains(action)
                && !action.equals("ORDER_STATUS") && !action.equals("RETURN_REFUND_STATUS")) {
            // CREATE/UPDATE/DELETE → chỉ có trong activity_log
            if (Set.of("CREATE", "UPDATE", "DELETE").contains(action)) return false;
        }
        return true;
    }

    /** Cần query activity_log không? */
    private static boolean needActivityLog(String source, String entityType, String action) {
        if (SOURCE_ORDER_HISTORY.equals(source)) return false;
        // nếu entityType rõ ràng là ORDER và không có gì khác → không cần activity_log
        if ("ORDER".equals(entityType) && action != null && ORDER_ACTIONS.contains(action)) return false;
        // nếu action là ORDER_STATUS / RETURN_REFUND → chỉ có trong order_history
        if (action != null && ORDER_ACTIONS.contains(action)) return false;
        return true;
    }

    /**
     * Chuyển action param sang changeType của order_history.
     * {@code null} = không lọc.
     */
    private static String toChangeType(String action) {
        if (action == null) return null;
        return switch (action) {
            case "ORDER_STATUS"         -> OrderHistoryEntity.CHANGE_TYPE_ORDER_STATUS;
            case "RETURN_REFUND_STATUS" -> OrderHistoryEntity.CHANGE_TYPE_RETURN_REFUND_STATUS;
            default -> null; // action không phải order-type → không lọc changeType
        };
    }

    /**
     * Chuyển action param sang action của admin_activity_log.
     * Chỉ truyền xuống nếu là CREATE/UPDATE/DELETE.
     */
    private static String toActivityLogAction(String action) {
        if (action == null) return null;
        return switch (action) {
            case "CREATE", "UPDATE", "DELETE" -> action;
            default -> null;
        };
    }
}
