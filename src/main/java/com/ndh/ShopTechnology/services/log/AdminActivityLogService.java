package com.ndh.ShopTechnology.services.log;

import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

public interface AdminActivityLogService {

    /**
     * Ghi một bản ghi vào admin_activity_log.
     */
    AdminActivityLogEntity log(String action,
                               String entityType,
                               Long entityId,
                               String entityLabel,
                               String snapshotBefore,
                               String snapshotAfter);

    /**
     * Lấy toàn bộ lịch sử của một entity cụ thể.
     */
    List<AdminActivityLogEntity> getByEntity(String entityType, Long entityId);

    /**
     * Tìm kiếm có lọc – dùng cho trang admin.
     */
    Page<AdminActivityLogEntity> search(Long actorUserId,
                                        String entityType,
                                        String action,
                                        Date from,
                                        Date to,
                                        Pageable pageable);
}
