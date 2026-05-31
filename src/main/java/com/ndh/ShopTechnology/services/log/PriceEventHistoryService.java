package com.ndh.ShopTechnology.services.log;

import com.ndh.ShopTechnology.entities.log.PriceEventHistoryEntity;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;

/**
 * Ghi và truy vấn lịch sử sự kiện các chương trình giá.
 */
public interface PriceEventHistoryService {

    // ── Ghi log PRICE_CHANGE ──────────────────────────────────────────────

    /**
     * Ghi log khi tạo mới đợt giá.
     * @param saved entity vừa được lưu
     */
    void logPriceChangeCreated(ProductPriceChangeEntity saved);

    /**
     * Ghi log khi cập nhật đợt giá.
     * @param before snapshot trước khi update
     * @param after  entity sau khi update
     */
    void logPriceChangeUpdated(ProductPriceChangeEntity before, ProductPriceChangeEntity after);

    /**
     * Ghi log khi xóa đợt giá.
     * @param deleted entity vừa bị xóa
     */
    void logPriceChangeDeleted(ProductPriceChangeEntity deleted);

    /**
     * Ghi log khi job tự động bắt đầu / kết thúc / hết quota đợt giá.
     * @param entity    đợt giá liên quan
     * @param eventType EVENT_STARTED | EVENT_ENDED | EVENT_EXPIRED
     */
    void logPriceChangeSystemEvent(ProductPriceChangeEntity entity, String eventType);

    // ── Ghi log VOLUME_TIER ───────────────────────────────────────────────

    /**
     * Ghi log khi admin thay thế toàn bộ bậc giá (bulk replace).
     * @param variantId  ID variant liên quan
     * @param productId  ID product liên quan
     */
    void logVolumeTierReplaced(Long variantId, Long productId);

    // ── Ghi log PWP_OFFER ─────────────────────────────────────────────────

    void logPwpCreated(PurchaseWithPurchaseOfferEntity saved);

    void logPwpUpdated(PurchaseWithPurchaseOfferEntity after);

    void logPwpDeleted(PurchaseWithPurchaseOfferEntity deleted);

    // ── Truy vấn ─────────────────────────────────────────────────────────

    Page<PriceEventHistoryEntity> search(
            String programType,
            Long programId,
            String eventType,
            Long productId,
            Date from,
            Date to,
            Pageable pageable);
}
