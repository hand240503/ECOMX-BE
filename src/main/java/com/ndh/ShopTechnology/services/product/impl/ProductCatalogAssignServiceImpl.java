package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportRowResult;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.services.importexport.ImportSupport;
import com.ndh.ShopTechnology.services.product.ProductCatalogAssignService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gán danh mục / thương hiệu cho sản phẩm hàng loạt (theo code). Mỗi dòng = một sản phẩm.
 * CHỈ cập nhật liên kết product → brand/category, đúng nguyên tắc "chỉ động vào dữ liệu của nó".
 */
@Service
public class ProductCatalogAssignServiceImpl implements ProductCatalogAssignService {

    private final ImportSupport support;
    private final ProductCatalogAssignPersister persister;

    public ProductCatalogAssignServiceImpl(ImportSupport support, ProductCatalogAssignPersister persister) {
        this.support = support;
        this.persister = persister;
    }

    private static final String C_SKU = "sku", C_ID = "productId",
            C_BRAND = "brandCode", C_CATEGORY = "categoryCode";

    private static final Map<String, String> ALIASES = buildAliases();

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"sku", "masku", "masanpham", "masp", "mahang", "productsku"}) m.put(a, C_SKU);
        for (String a : new String[]{"productid", "id", "idsanpham"}) m.put(a, C_ID);
        for (String a : new String[]{"brandcode", "brand", "mahang", "mathuonghieu", "thuonghieu", "hang", "codebrand"}) m.putIfAbsent(a, C_BRAND);
        for (String a : new String[]{"categorycode", "category", "madanhmuc", "danhmuc", "madm", "codecategory"}) m.putIfAbsent(a, C_CATEGORY);
        return m;
    }

    private static final String[] TEMPLATE_HEADERS = {"sku", "brand_code", "category_code"};

    @Override
    public CatalogImportResponse importAssign(MultipartFile file) {
        List<String[]> rows = support.readRows(file);
        Map<Integer, String> col = support.mapHeader(rows.get(0), ALIASES);
        if (!col.containsValue(C_SKU) && !col.containsValue(C_ID)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'sku' hoặc 'product_id' ở dòng tiêu đề");
        }
        if (!col.containsValue(C_BRAND) && !col.containsValue(C_CATEGORY)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'brand_code' hoặc 'category_code' ở dòng tiêu đề");
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
            String brandCode = support.get(row, col, C_BRAND);
            String categoryCode = support.get(row, col, C_CATEGORY);
            String key = sku != null ? ("sku=" + sku) : ("id=" + id);
            try {
                if (sku == null && id == null) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST, "Thiếu sku và product_id");
                }
                ProductCatalogAssignPersister.Outcome o = persister.assign(sku, id, brandCode, categoryCode);
                resp.setTotalRows(resp.getTotalRows() + 1);
                if ("SKIPPED".equals(o.action)) {
                    resp.setSkippedCount(resp.getSkippedCount() + 1);
                    resp.getResults().add(CatalogImportRowResult.builder()
                            .rowNumber(excelRow).key(key).action("SKIPPED").success(true).id(o.id)
                            .message("Không có thay đổi — bỏ qua").build());
                } else {
                    resp.setUpdatedCount(resp.getUpdatedCount() + 1);
                    resp.getResults().add(CatalogImportRowResult.builder()
                            .rowNumber(excelRow).key(key).action("UPDATED").success(true).id(o.id)
                            .message("Đã cập nhật danh mục/thương hiệu").build());
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
                {"100023", "DELL", "LAPTOP"},
                {"100050", "LOGITECH", "PHU_KIEN"},
                {"100099", "", "MAN_HINH"},
        };
        String[] guide = {
                "HƯỚNG DẪN GÁN DANH MỤC / THƯƠNG HIỆU HÀNG LOẠT",
                "",
                "1. Mỗi DÒNG = một sản phẩm. Xác định sản phẩm bằng sku (ưu tiên) hoặc product_id.",
                "2. brand_code / category_code: điền CODE của thương hiệu / danh mục ĐÃ TỒN TẠI.",
                "   (Tạo thương hiệu/danh mục trước bằng chức năng Hãng / Danh mục.)",
                "3. Cột để TRỐNG = giữ nguyên liên kết hiện tại (không thay đổi).",
                "4. Code không tồn tại -> dòng đó báo lỗi, các dòng khác vẫn chạy.",
                "5. Chức năng này CHỈ đổi danh mục/thương hiệu, không sửa thông tin khác của sản phẩm.",
        };
        return ImportSupport.buildTemplate("Gan DM - Thuong hieu", TEMPLATE_HEADERS, examples, guide);
    }
}
