package com.ndh.ShopTechnology.services.log;

import com.ndh.ShopTechnology.dto.response.log.UnifiedHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;

public interface UnifiedHistoryService {

    /**
     * Tìm kiếm lịch sử hệ thống gộp từ nhiều nguồn.
     *
     * @param source         Nguồn dữ liệu: {@code ALL} | {@code ORDER_HISTORY} | {@code ACTIVITY_LOG}
     *                       (null = ALL)
     * @param entityType     Loại entity cần lọc: ORDER | PRODUCT | BRAND | CATEGORY |
     *                       PRICE_CHANGE | VOLUME_TIER | PWP_OFFER (null = tất cả)
     * @param entityId       ID của entity cụ thể (null = tất cả)
     * @param actorUserId    ID người thực hiện (null = tất cả)
     * @param actorRoleCode  Role code của người thực hiện, vd: {@code "EMPLOYEE"} (null = tất cả)
     * @param action         Hành động: CREATE | UPDATE | DELETE |
     *                       ORDER_STATUS | RETURN_REFUND_STATUS (null = tất cả)
     * @param from           Lọc từ thời điểm (null = không giới hạn đầu)
     * @param to             Lọc đến thời điểm (null = không giới hạn cuối)
     * @param pageable       Phân trang và sắp xếp
     * @return Trang kết quả {@link UnifiedHistoryResponse} sắp xếp theo {@code createdAt DESC}
     */
    Page<UnifiedHistoryResponse> search(
            String   source,
            String   entityType,
            Long     entityId,
            Long     actorUserId,
            String   actorRoleCode,
            String   action,
            Date     from,
            Date     to,
            Pageable pageable);
}
