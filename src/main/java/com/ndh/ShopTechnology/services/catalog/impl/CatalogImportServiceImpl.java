package com.ndh.ShopTechnology.services.catalog.impl;

import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportRowResult;
import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.services.catalog.CatalogImportService;
import com.ndh.ShopTechnology.utils.SpreadsheetReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogImportServiceImpl implements CatalogImportService {

    private final CatalogUpsertPersister persister;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public CatalogImportServiceImpl(CatalogUpsertPersister persister,
                                    BrandRepository brandRepository,
                                    CategoryRepository categoryRepository) {
        this.persister = persister;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    private static final String C_ID = "id", C_CODE = "code", C_NAME = "name", C_STATUS = "status",
            C_PARENT_ID = "parentId", C_PARENT_NAME = "parentName", C_PARENT_CODE = "parentCode";

    private static final Map<String, String> ALIASES = buildAliases();

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"id"}) m.put(a, C_ID);
        for (String a : new String[]{"code", "ma", "macode"}) m.put(a, C_CODE);
        for (String a : new String[]{"name", "ten", "tendanhmuc", "tenhang", "tenthuonghieu", "tennhom"}) m.put(a, C_NAME);
        for (String a : new String[]{"status", "trangthai"}) m.put(a, C_STATUS);
        for (String a : new String[]{"parentcode", "macha", "codecha", "madanhmuccha", "machadm"}) m.put(a, C_PARENT_CODE);
        for (String a : new String[]{"parentid", "danhmucchaid", "parent", "idcha"}) m.put(a, C_PARENT_ID);
        for (String a : new String[]{"parentname", "danhmuccha", "tendanhmuccha", "tencha"}) m.put(a, C_PARENT_NAME);
        return m;
    }

    @Override
    public CatalogImportResponse importBrands(MultipartFile file) {
        Ctx ctx = parse(file);
        CatalogImportResponse resp = newResponse();
        for (int i = 1; i < ctx.rows.size(); i++) {
            String[] row = ctx.rows.get(i);
            if (SpreadsheetReader.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String code = get(row, ctx.col, C_CODE);
            String name = get(row, ctx.col, C_NAME);
            String key = !isBlank(code) ? code : name;
            try {
                CatalogUpsertPersister.Outcome o = persister.upsertBrand(
                        parseLong(get(row, ctx.col, C_ID)), code, name, parseInt(get(row, ctx.col, C_STATUS)));
                record(resp, excelRow, key, o);
            } catch (Exception e) {
                recordFail(resp, excelRow, key, e);
            }
        }
        return resp;
    }

    @Override
    public CatalogImportResponse importCategories(MultipartFile file) {
        Ctx ctx = parse(file);
        CatalogImportResponse resp = newResponse();
        for (int i = 1; i < ctx.rows.size(); i++) {
            String[] row = ctx.rows.get(i);
            if (SpreadsheetReader.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String code = get(row, ctx.col, C_CODE);
            String name = get(row, ctx.col, C_NAME);
            String key = !isBlank(code) ? code : name;
            try {
                CatalogUpsertPersister.Outcome o = persister.upsertCategory(
                        parseLong(get(row, ctx.col, C_ID)), code, name,
                        parseInt(get(row, ctx.col, C_STATUS)),
                        parseLong(get(row, ctx.col, C_PARENT_ID)),
                        get(row, ctx.col, C_PARENT_CODE),
                        get(row, ctx.col, C_PARENT_NAME));
                record(resp, excelRow, key, o);
            } catch (Exception e) {
                recordFail(resp, excelRow, key, e);
            }
        }
        return resp;
    }

    // ── Preview (dry-run, không ghi CSDL) ────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CatalogImportResponse previewBrands(MultipartFile file) {
        Ctx ctx = parse(file);
        CatalogImportResponse resp = newResponse();
        for (int i = 1; i < ctx.rows.size(); i++) {
            String[] row = ctx.rows.get(i);
            if (SpreadsheetReader.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String code = get(row, ctx.col, C_CODE);
            String name = get(row, ctx.col, C_NAME);
            String key = !isBlank(code) ? code : name;
            if (isBlank(name)) {
                recordPreviewFail(resp, excelRow, key, "Thiếu tên (name)");
                continue;
            }
            Long id = parseLong(get(row, ctx.col, C_ID));
            BrandEntity existing = null;
            if (id != null) existing = brandRepository.findById(id).orElse(null);
            if (existing == null && !isBlank(code)) {
                existing = brandRepository.findFirstByCodeIgnoreCase(code.trim()).orElse(null);
            }
            recordPreview(resp, excelRow, key, existing != null, existing != null ? existing.getId() : null);
        }
        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogImportResponse previewCategories(MultipartFile file) {
        Ctx ctx = parse(file);
        CatalogImportResponse resp = newResponse();
        for (int i = 1; i < ctx.rows.size(); i++) {
            String[] row = ctx.rows.get(i);
            if (SpreadsheetReader.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String code = get(row, ctx.col, C_CODE);
            String name = get(row, ctx.col, C_NAME);
            String key = !isBlank(code) ? code : name;
            if (isBlank(name)) {
                recordPreviewFail(resp, excelRow, key, "Thiếu tên (name)");
                continue;
            }
            Long parentId = parseLong(get(row, ctx.col, C_PARENT_ID));
            if (parentId != null && !categoryRepository.existsById(parentId)) {
                recordPreviewFail(resp, excelRow, key, "Không tìm thấy danh mục cha id=" + parentId);
                continue;
            }
            String parentCode = get(row, ctx.col, C_PARENT_CODE);
            if (parentId == null && !isBlank(parentCode)
                    && categoryRepository.findFirstByCodeIgnoreCase(parentCode.trim()).isEmpty()) {
                recordPreviewFail(resp, excelRow, key, "Không tìm thấy danh mục cha code=" + parentCode);
                continue;
            }
            Long id = parseLong(get(row, ctx.col, C_ID));
            CategoryEntity existing = null;
            if (id != null) existing = categoryRepository.findById(id).orElse(null);
            if (existing == null && !isBlank(code)) {
                existing = categoryRepository.findFirstByCodeIgnoreCase(code.trim()).orElse(null);
            }
            recordPreview(resp, excelRow, key, existing != null, existing != null ? existing.getId() : null);
        }
        return resp;
    }

    private void recordPreview(CatalogImportResponse resp, int rowNum, String key, boolean exists, Long id) {
        resp.setTotalRows(resp.getTotalRows() + 1);
        if (exists) resp.setUpdatedCount(resp.getUpdatedCount() + 1);
        else resp.setCreatedCount(resp.getCreatedCount() + 1);
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action(exists ? "UPDATED" : "CREATED").success(true).id(id)
                .message(exists ? "Sẽ cập nhật (ghi đè)" : "Sẽ thêm mới")
                .build());
    }

    private void recordPreviewFail(CatalogImportResponse resp, int rowNum, String key, String msg) {
        resp.setTotalRows(resp.getTotalRows() + 1);
        resp.setFailureCount(resp.getFailureCount() + 1);
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action("FAILED").success(false).message(msg)
                .build());
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static class Ctx {
        List<String[]> rows;
        Map<Integer, String> col;
    }

    private Ctx parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "File rỗng hoặc chưa được chọn");
        }
        List<String[]> rows;
        try {
            rows = SpreadsheetReader.read(file);
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Không đọc được file: " + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "File không có dữ liệu");
        }
        Map<Integer, String> col = new HashMap<>();
        String[] header = rows.get(0);
        for (int i = 0; i < header.length; i++) {
            String key = ALIASES.get(SpreadsheetReader.normalizeHeader(header[i]));
            if (key != null && !col.containsValue(key)) col.put(i, key);
        }
        if (!col.containsValue(C_NAME) && !col.containsValue(C_CODE)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'name' hoặc 'code' ở dòng tiêu đề");
        }
        Ctx ctx = new Ctx();
        ctx.rows = rows;
        ctx.col = col;
        return ctx;
    }

    private static CatalogImportResponse newResponse() {
        return CatalogImportResponse.builder()
                .totalRows(0).createdCount(0).updatedCount(0).failureCount(0)
                .results(new java.util.ArrayList<>())
                .build();
    }

    private void record(CatalogImportResponse resp, int rowNum, String key, CatalogUpsertPersister.Outcome o) {
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
            message = "Đã cập nhật (ghi đè)";
        }
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action(o.action).success(true).id(o.id)
                .message(message)
                .build());
    }

    private void recordFail(CatalogImportResponse resp, int rowNum, String key, Throwable e) {
        resp.setTotalRows(resp.getTotalRows() + 1);
        resp.setFailureCount(resp.getFailureCount() + 1);
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action("FAILED").success(false)
                .message(rootMessage(e))
                .build());
    }

    private String get(String[] row, Map<Integer, String> col, String key) {
        for (Map.Entry<Integer, String> e : col.entrySet()) {
            if (e.getValue().equals(key)) {
                int idx = e.getKey();
                return idx < row.length && row[idx] != null ? row[idx].trim() : "";
            }
        }
        return "";
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private Long parseLong(String s) {
        Double d = parseNumber(s);
        return d == null ? null : d.longValue();
    }

    private Integer parseInt(String s) {
        Double d = parseNumber(s);
        return d == null ? null : (int) Math.round(d);
    }

    private Double parseNumber(String s) {
        if (isBlank(s)) return null;
        String t = s.trim().replaceAll("(?i)[a-zđ₫\\s]", "");
        if (t.isEmpty()) return null;
        boolean dot = t.indexOf('.') >= 0, comma = t.indexOf(',') >= 0;
        if (dot && comma) {
            char dec = t.lastIndexOf('.') > t.lastIndexOf(',') ? '.' : ',';
            t = t.replace(String.valueOf(dec == '.' ? ',' : '.'), "").replace(dec, '.');
        } else if (comma) {
            t = t.matches("-?\\d{1,3}(,\\d{3})+") ? t.replace(",", "") : t.replace(',', '.');
        } else if (dot) {
            if (t.matches("-?\\d{1,3}(\\.\\d{3})+")) t = t.replace(".", "");
        }
        try { return Double.parseDouble(t); } catch (NumberFormatException e) { return null; }
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        String msg = cur.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getMessage();
        return msg != null ? msg : "Lỗi không xác định";
    }
}
