package com.ndh.ShopTechnology.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.annotation.AdminAudit;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import com.ndh.ShopTechnology.services.log.AdminActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect tự động ghi admin_activity_log cho mọi method được đánh
 * {@link AdminAudit}.
 *
 * <p>Flow:
 * <ol>
 *   <li>Nếu {@code captureSnapshotBefore = true}: lấy snapshot JSON trước khi thực hiện
 *       thông qua {@link SnapshotFetcherRegistry}.</li>
 *   <li>Thực thi method gốc.</li>
 *   <li>Serialize kết quả trả về làm {@code snapshotAfter} (với CREATE / UPDATE).</li>
 *   <li>Gọi {@link AdminActivityLogService#log} với thông tin đã thu thập.</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminActivityLogAspect {

    private final AdminActivityLogService adminActivityLogService;
    private final SnapshotFetcherRegistry snapshotFetcherRegistry;
    private final ObjectMapper objectMapper;

    @Around("@annotation(adminAudit)")
    public Object around(ProceedingJoinPoint pjp, AdminAudit adminAudit) throws Throwable {

        String entityType = adminAudit.entityType();
        String action     = adminAudit.action();
        int    idArgIdx   = adminAudit.idArgIndex();

        // 1. Lấy entity ID từ args (dùng cho UPDATE / DELETE)
        Long entityId = extractEntityId(pjp.getArgs(), idArgIdx);

        // 2. Lấy snapshot trước khi thực hiện (cho UPDATE / DELETE)
        String snapshotBefore = null;
        if (adminAudit.captureSnapshotBefore() && entityId != null) {
            snapshotBefore = snapshotFetcherRegistry.fetch(entityType, entityId);
        }

        // 3. Thực thi method gốc
        Object result = pjp.proceed();

        // 4. Snapshot sau khi thực hiện (CREATE / UPDATE có kết quả trả về)
        String snapshotAfter = null;
        if (!AdminActivityLogEntity.ACTION_DELETE.equals(action) && result != null) {
            snapshotAfter = toJson(result);
        }

        // 5. Nếu là CREATE và chưa có entityId, thử lấy từ return value
        if (entityId == null && result != null) {
            entityId = tryExtractId(result);
        }

        // 6. Lấy label (tên thân thiện của entity)
        String entityLabel = tryExtractLabel(result, pjp.getArgs());

        // 7. Ghi log
        adminActivityLogService.log(action, entityType, entityId, entityLabel,
                snapshotBefore, snapshotAfter);

        return result;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Lấy Long từ args[idArgIdx], bỏ qua nếu index âm hoặc vượt độ dài */
    private Long extractEntityId(Object[] args, int idArgIdx) {
        if (idArgIdx < 0 || args == null || idArgIdx >= args.length) return null;
        Object arg = args[idArgIdx];
        if (arg instanceof Long)    return (Long) arg;
        if (arg instanceof Integer) return ((Integer) arg).longValue();
        if (arg instanceof Number)  return ((Number) arg).longValue();
        return null;
    }

    /** Cố gắng đọc field "id" từ return value qua Jackson (dùng cho CREATE) */
    private Long tryExtractId(Object result) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    objectMapper.valueToTree(result);
            if (node.has("id") && !node.get("id").isNull()) {
                return node.get("id").asLong();
            }
        } catch (Exception ignored) { }
        return null;
    }

    /** Cố gắng đọc field "name" hoặc "productName" từ return value */
    private String tryExtractLabel(Object result, Object[] args) {
        if (result == null) return null;
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    objectMapper.valueToTree(result);
            for (String field : new String[]{"name", "productName", "code", "orderCode"}) {
                if (node.has(field) && node.get(field).isTextual()) {
                    return node.get(field).asText();
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[AdminActivityLogAspect] Cannot serialize result: {}", e.getMessage());
            return null;
        }
    }
}
