package com.ndh.ShopTechnology.services.unit.impl;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportRowResult;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.services.importexport.ImportSupport;
import com.ndh.ShopTechnology.services.unit.UnitImportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import đơn vị tính. Upsert theo khóa id → code → name_unit; dòng không thay đổi sẽ BỎ QUA.
 * Không xóa đơn vị vắng mặt trong file (chính sách "missing = giữ nguyên").
 */
@Service
public class UnitImportServiceImpl implements UnitImportService {

    private final ImportSupport support;
    private final UnitUpsertPersister persister;

    public UnitImportServiceImpl(ImportSupport support, UnitUpsertPersister persister) {
        this.support = support;
        this.persister = persister;
    }

    private static final String C_ID = "id", C_CODE = "code", C_NAME = "name", C_RATIO = "ratio", C_STATUS = "status";

    private static final Map<String, String> ALIASES = buildAliases();

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"id"}) m.put(a, C_ID);
        for (String a : new String[]{"code", "ma", "macode", "madonvi"}) m.put(a, C_CODE);
        for (String a : new String[]{"name", "nameunit", "ten", "tendonvi", "donvi", "dvt", "donvitinh"}) m.put(a, C_NAME);
        for (String a : new String[]{"ratio", "tyle", "hesoquydoi", "heso", "quydoi"}) m.put(a, C_RATIO);
        for (String a : new String[]{"status", "trangthai"}) m.put(a, C_STATUS);
        return m;
    }

    private static final String[] TEMPLATE_HEADERS = {"id", "code", "name_unit", "ratio", "status"};

    @Override
    public CatalogImportResponse importUnits(MultipartFile file) {
        List<String[]> rows = support.readRows(file);
        Map<Integer, String> col = support.mapHeader(rows.get(0), ALIASES);
        if (!col.containsValue(C_NAME)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'name_unit' (tên đơn vị) ở dòng tiêu đề");
        }

        CatalogImportResponse resp = CatalogImportResponse.builder()
                .totalRows(0).createdCount(0).updatedCount(0).skippedCount(0).failureCount(0)
                .results(new ArrayList<>()).build();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (support.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String code = support.get(row, col, C_CODE);
            String name = support.get(row, col, C_NAME);
            String key = !support.isBlank(code) ? code : name;
            try {
                UnitUpsertPersister.Outcome o = persister.upsert(
                        support.parseLong(support.get(row, col, C_ID)),
                        code, name,
                        support.parseInt(support.get(row, col, C_RATIO)),
                        support.parseInt(support.get(row, col, C_STATUS)));
                record(resp, excelRow, key, o);
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

    private void record(CatalogImportResponse resp, int rowNum, String key, UnitUpsertPersister.Outcome o) {
        resp.setTotalRows(resp.getTotalRows() + 1);
        String message;
        if ("CREATED".equals(o.action)) {
            resp.setCreatedCount(resp.getCreatedCount() + 1);
            message = "Đã thêm mới";
        } else if ("SKIPPED".equals(o.action)) {
            resp.setSkippedCount(resp.getSkippedCount() + 1);
            message = "Bỏ qua (không thay đổi)";
        } else {
            resp.setUpdatedCount(resp.getUpdatedCount() + 1);
            message = "Đã cập nhật";
        }
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action(o.action).success(true).id(o.id).message(message)
                .build());
    }

    @Override
    public byte[] buildTemplateXlsx() {
        String[][] examples = {
                {"", "CAI", "Cái", "1", "1"},
                {"", "HOP6", "Hộp 6", "6", "1"},
                {"", "THUNG24", "Thùng 24", "24", "1"},
        };
        String[] guide = {
                "HƯỚNG DẪN IMPORT ĐƠN VỊ TÍNH (UNIT)",
                "",
                "1. Mỗi DÒNG = một đơn vị tính.",
                "2. Khóa định danh để cập nhật: id (ưu tiên) → code → name_unit.",
                "   - Có id/code khớp đơn vị đã có => CẬP NHẬT.",
                "   - Không khớp => THÊM MỚI. Để trống code thì hệ thống tự sinh từ tên.",
                "3. name_unit (bắt buộc): tên đơn vị, ví dụ 'Cái', 'Hộp 6'.",
                "4. ratio: hệ số quy đổi (số nguyên >= 1, mặc định 1).",
                "5. status: 1 = đang dùng, 0 = ngưng (mặc định 1).",
                "6. Dòng có dữ liệu GIỐNG HỆT đơn vị hiện có sẽ được BỎ QUA (không ghi đè).",
                "7. Đơn vị không xuất hiện trong file sẽ được GIỮ NGUYÊN (import không xóa).",
        };
        return ImportSupport.buildTemplate("Don vi tinh", TEMPLATE_HEADERS, examples, guide);
    }
}
