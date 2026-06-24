package com.ndh.ShopTechnology.services.inventory.impl;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportRowResult;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.services.importexport.ImportSupport;
import com.ndh.ShopTechnology.services.inventory.InventoryImportService;
import com.ndh.ShopTechnology.services.inventory.InventoryService;
import com.ndh.ShopTechnology.utils.SpreadsheetReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import tồn kho từ file. Gọi lại {@link InventoryService} cho từng dòng nên mỗi dòng
 * chạy trong transaction riêng và ghi sổ cái (ledger) đầy đủ như thao tác thủ công.
 */
@Service
public class InventoryImportServiceImpl implements InventoryImportService {

    private final InventoryService inventoryService;
    private final ImportSupport support;

    public InventoryImportServiceImpl(InventoryService inventoryService, ImportSupport support) {
        this.inventoryService = inventoryService;
        this.support = support;
    }

    private static final String C_VARIANT_ID = "variantId", C_SKU = "skuCode",
            C_QUANTITY = "quantity", C_MODE = "mode", C_NOTE = "note";

    private static final Map<String, String> ALIASES = buildAliases();

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"variantid", "idbienthe", "mabienthe", "variant"}) m.put(a, C_VARIANT_ID);
        for (String a : new String[]{"skucode", "sku", "mabienthe2", "skubienthe", "masku"}) m.put(a, C_SKU);
        for (String a : new String[]{"quantity", "soluong", "sl", "qty", "soluongnhap"}) m.put(a, C_QUANTITY);
        for (String a : new String[]{"mode", "che do", "chedo", "kieu", "loai", "hinhthuc"}) m.put(a, C_MODE);
        for (String a : new String[]{"note", "ghichu", "lyrdo", "lydo", "ghi chu"}) m.put(a, C_NOTE);
        return m;
    }

    private static final String[] TEMPLATE_HEADERS = {"variant_id", "sku_code", "quantity", "mode", "note"};

    @Override
    public CatalogImportResponse importStock(MultipartFile file) {
        List<String[]> rows = support.readRows(file);
        Map<Integer, String> col = support.mapHeader(rows.get(0), ALIASES);
        if (!col.containsValue(C_VARIANT_ID) && !col.containsValue(C_SKU)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'variant_id' hoặc 'sku_code' ở dòng tiêu đề");
        }
        if (!col.containsValue(C_QUANTITY)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy cột 'quantity' ở dòng tiêu đề");
        }

        CatalogImportResponse resp = CatalogImportResponse.builder()
                .totalRows(0).createdCount(0).updatedCount(0).failureCount(0)
                .results(new ArrayList<>()).build();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (support.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String idStr = support.get(row, col, C_VARIANT_ID);
            String sku = support.get(row, col, C_SKU);
            String key = !support.isBlank(sku) ? sku : ("id=" + idStr);
            try {
                ProductVariantEntity v = support.resolveVariant(idStr, sku, null);
                Integer qty = support.parseInt(support.get(row, col, C_QUANTITY));
                if (qty == null) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST, "Thiếu số lượng (quantity)");
                }
                boolean setMode = isSetMode(support.get(row, col, C_MODE));
                String note = support.nullIfBlank(support.get(row, col, C_NOTE));
                String action;
                if (setMode) {
                    if (qty < 0) throw new CustomApiException(HttpStatus.BAD_REQUEST, "Tồn kho đặt không được âm");
                    inventoryService.adjustOnHand(v.getId(), qty, note != null ? note : "Import kiểm kê");
                    action = "UPDATED";
                } else {
                    if (qty <= 0) throw new CustomApiException(HttpStatus.BAD_REQUEST, "Số lượng nhập phải > 0");
                    inventoryService.importStock(v.getId(), qty, note != null ? note : "Import nhập kho");
                    action = "CREATED";
                }
                resp.setTotalRows(resp.getTotalRows() + 1);
                if ("CREATED".equals(action)) resp.setCreatedCount(resp.getCreatedCount() + 1);
                else resp.setUpdatedCount(resp.getUpdatedCount() + 1);
                resp.getResults().add(CatalogImportRowResult.builder()
                        .rowNumber(excelRow).key(key).action(action).success(true).id(v.getId())
                        .message(setMode ? "Đã đặt tồn = " + qty : "Đã nhập thêm " + qty)
                        .build());
            } catch (Exception e) {
                resp.setTotalRows(resp.getTotalRows() + 1);
                resp.setFailureCount(resp.getFailureCount() + 1);
                resp.getResults().add(CatalogImportRowResult.builder()
                        .rowNumber(excelRow).key(key).action("FAILED").success(false)
                        .message(ImportSupport.rootMessage(e))
                        .build());
            }
        }
        return resp;
    }

    /** mode rỗng/“add/import/nhap/cong” => cộng thêm; “set/adjust/dat/kiemke” => đặt tuyệt đối. */
    private boolean isSetMode(String raw) {
        String t = SpreadsheetReader.normalizeHeader(raw);
        switch (t) {
            case "set": case "adjust": case "dat": case "kiemke": case "dattuyetdoi": case "tuyetdoi": case "ghide":
                return true;
            default:
                return false;
        }
    }

    @Override
    public byte[] buildTemplateXlsx() {
        String[][] examples = {
                {"", "DELL-XPS13-I7", "50", "add", "Nhập lô đầu kỳ"},
                {"", "DELL-XPS13-I5", "30", "add", "Nhập thêm"},
                {"1024", "", "100", "set", "Kiểm kê đặt lại tồn"},
        };
        String[] guide = {
                "HƯỚNG DẪN IMPORT TỒN KHO",
                "",
                "1. Mỗi DÒNG = một biến thể (SKU) cần cập nhật tồn kho.",
                "2. Xác định biến thể bằng MỘT trong hai: variant_id (id biến thể) HOẶC sku_code (mã SKU biến thể).",
                "   Nếu điền cả hai, hệ thống ưu tiên variant_id.",
                "3. quantity: số lượng (số nguyên).",
                "4. mode: 'add' = CỘNG THÊM vào tồn hiện có (nhập kho, quantity > 0);",
                "          'set' = ĐẶT TUYỆT ĐỐI tồn về đúng số này (kiểm kê, quantity >= 0).",
                "   Để trống mode mặc định là 'add'.",
                "5. note: ghi chú (tùy chọn) — lưu vào sổ cái kho.",
                "6. Mỗi dòng xử lý độc lập và ghi sổ cái (ledger). Dòng lỗi được báo cáo, không chặn cả file.",
        };
        return ImportSupport.buildTemplate("Ton kho", TEMPLATE_HEADERS, examples, guide);
    }
}
