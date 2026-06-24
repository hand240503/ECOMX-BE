package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Import các chương trình giá/khuyến mãi từ file Excel/CSV/TXT:
 * <ul>
 *   <li>PC — Price Change (đổi giá theo thời gian, theo biến thể).</li>
 *   <li>PWP — Purchase With Purchase (mua kèm giá ưu đãi).</li>
 *   <li>Mix &amp; Match — Volume Price Tier (giá theo số lượng, theo biến thể).</li>
 * </ul>
 * Biến thể tra theo variant_id hoặc sku_code. Mỗi dòng/biến thể xử lý độc lập;
 * dòng lỗi được báo cáo lại, không chặn cả file.
 */
public interface PromotionImportService {

    CatalogImportResponse importPriceChanges(MultipartFile file);

    CatalogImportResponse importPurchaseWithPurchase(MultipartFile file);

    CatalogImportResponse importVolumeTiers(MultipartFile file);

    byte[] buildPriceChangeTemplate();

    byte[] buildPurchaseWithPurchaseTemplate();

    byte[] buildVolumeTierTemplate();
}
