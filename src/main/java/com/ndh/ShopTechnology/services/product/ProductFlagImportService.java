package com.ndh.ShopTechnology.services.product;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import org.springframework.web.multipart.MultipartFile;

/** Import Excel/CSV để ĐÁNH DẤU sản phẩm là nổi bật (featured) hoặc hot-sale. */
public interface ProductFlagImportService {

    /** Bật cờ nổi bật cho các sản phẩm trong file. */
    CatalogImportResponse importFeatured(MultipartFile file);

    /** Bật cờ hot-sale cho các sản phẩm trong file. */
    CatalogImportResponse importHotSale(MultipartFile file);

    /** File Excel mẫu (chung cho cả hai: cột sku + product_id). */
    byte[] buildTemplateXlsx();
}
