package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportRowResult;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.services.importexport.ImportSupport;
import com.ndh.ShopTechnology.services.product.ProductFlagImportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import đánh dấu sản phẩm nổi bật / hot-sale. Mỗi dòng = một sản phẩm cần BẬT cờ.
 * Tra theo sku (ưu tiên) rồi product_id. Sản phẩm đã bật cờ -> BỎ QUA (skipped).
 */
@Service
public class ProductFlagImportServiceImpl implements ProductFlagImportService {

    private final ImportSupport support;
    private final ProductFlagPersister persister;

    public ProductFlagImportServiceImpl(ImportSupport support, ProductFlagPersister persister) {
        this.support = support;
        this.persister = persister;
    }

    private static final String C_SKU = "sku", C_ID = "productId";

    private static final Map<String, String> ALIASES = buildAliases();

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"sku", "masku", "masanpham", "masp", "mahang", "productsku", "skusanpham"}) m.put(a, C_SKU);
        for (String a : new String[]{"productid", "id", "idsanpham", "masanphamid"}) m.put(a, C_ID);
        return m;
    }

    private static final String[] TEMPLATE_HEADERS = {"sku", "product_id"};

    @Override
    public CatalogImportResponse importFeatured(MultipartFile file) {
        return run(file, ProductFlagPersister.Flag.FEATURED, "nổi bật");
    }

    @Override
    public CatalogImportResponse importHotSale(MultipartFile file) {
        return run(file, ProductFlagPersister.Flag.HOT_SALE, "hot-sale");
    }

    private CatalogImportResponse run(MultipartFile file, ProductFlagPersister.Flag flag, String label) {
        List<String[]> rows = support.readRows(file);
        Map<Integer, String> col = support.mapHeader(rows.get(0), ALIASES);
        if (!col.containsValue(C_SKU) && !col.containsValue(C_ID)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'sku' hoặc 'product_id' ở dòng tiêu đề");
        }

        CatalogImportResponse resp = CatalogImportResponse.builder()
                .totalRows(0).createdCount(0).updatedCount(0).skippedCount(0).failureCount(0)
                .results(new ArrayList<>()).build();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (support.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            Long sku = support.parseLong(support.get(row, col, C_SKU));
            Long id = support.parseLong(support.get(row, col, C_ID));
            String key = sku != null ? ("sku=" + sku) : ("id=" + id);
            try {
                if (sku == null && id == null) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST, "Thiếu sku và product_id");
                }
                ProductFlagPersister.Outcome o = persister.setFlag(id, sku, flag);
                resp.setTotalRows(resp.getTotalRows() + 1);
                if ("SKIPPED".equals(o.action)) {
                    resp.setSkippedCount(resp.getSkippedCount() + 1);
                    resp.getResults().add(CatalogImportRowResult.builder()
                            .rowNumber(excelRow).key(key).action("SKIPPED").success(true).id(o.id)
                            .message("Đã là " + label + " từ trước").build());
                } else {
                    resp.setCreatedCount(resp.getCreatedCount() + 1);
                    resp.getResults().add(CatalogImportRowResult.builder()
                            .rowNumber(excelRow).key(key).action("CREATED").success(true).id(o.id)
                            .message("Đã đánh dấu " + label).build());
                }
            } catch (Exception e) {
                resp.setTotalRows(resp.getTotalRows() + 1);
                resp.setFailureCount(resp.getFailureCount() + 1);
                resp.getResults().add(CatalogImportRowResult.builder()
                        .rowNumber(excelRow).key(key).action("FAILED").success(false)
                        .message(ImportSupport.rootMessage(e)).build());
            }
        }
        return resp;
    }

    @Override
    public byte[] buildTemplateXlsx() {
        String[][] examples = {
                {"1024", ""},
                {"1090", ""},
                {"", "55"},
        };
        String[] guide = {
                "HƯỚNG DẪN IMPORT ĐÁNH DẤU NỔI BẬT / HOT-SALE",
                "",
                "1. Mỗi DÒNG = một sản phẩm cần BẬT cờ (nổi bật hoặc hot-sale tùy nút bạn bấm).",
                "2. Xác định sản phẩm bằng MỘT trong hai: sku (mã SP dạng số) HOẶC product_id.",
                "   Nếu điền cả hai, hệ thống ưu tiên sku.",
                "3. File CHỈ để THÊM (bật cờ = true). Sản phẩm đã bật sẵn sẽ được BỎ QUA.",
                "4. Để GỠ cờ, dùng giao diện chỉnh sửa sản phẩm (import này không tắt cờ).",
        };
        return ImportSupport.buildTemplate("Noi bat - Hot sale", TEMPLATE_HEADERS, examples, guide);
    }
}
