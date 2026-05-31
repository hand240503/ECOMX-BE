package com.ndh.ShopTechnology.aspect;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registry cho phép mỗi service đăng ký một hàm lấy snapshot JSON
 * của entity theo ID – dùng bởi AdminActivityLogAspect để lấy trạng thái
 * TRƯỚC khi UPDATE / DELETE.
 *
 * <p>Cách dùng trong service impl (trong @PostConstruct hoặc constructor):
 * <pre>
 *   snapshotFetcherRegistry.register(AdminActivityLogEntity.ENTITY_BRAND, id -> {
 *       return brandRepository.findById(id)
 *           .map(e -> objectMapper.writeValueAsString(BrandResponse.fromEntity(e, null)))
 *           .orElse(null);
 *   });
 * </pre>
 */
@Component
public class SnapshotFetcherRegistry {

    private final Map<String, Function<Long, String>> fetchers = new HashMap<>();

    /**
     * Đăng ký fetcher cho một loại entity.
     * @param entityType  hằng số ENTITY_* trong AdminActivityLogEntity
     * @param fetcher     nhận entityId, trả về JSON string hoặc null
     */
    public void register(String entityType, Function<Long, String> fetcher) {
        fetchers.put(entityType, fetcher);
    }

    /**
     * Lấy snapshot JSON của entity. Trả về null nếu không có fetcher
     * hoặc entity không tồn tại.
     */
    public String fetch(String entityType, Long entityId) {
        if (entityId == null) return null;
        Function<Long, String> fetcher = fetchers.get(entityType);
        if (fetcher == null) return null;
        try {
            return fetcher.apply(entityId);
        } catch (Exception e) {
            return null;
        }
    }
}
