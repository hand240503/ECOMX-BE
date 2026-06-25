package com.ndh.ShopTechnology.services.product.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.dto.response.product.VariantImportResponse;
import com.ndh.ShopTechnology.dto.response.product.VariantImportRowResult;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.product.ProductVariantImportService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductVariantImportServiceImpl implements ProductVariantImportService {

    private final ProductVariantImportPersister persister;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public ProductVariantImportServiceImpl(ProductVariantImportPersister persister,
                                           ProductRepository productRepository,
                                           ObjectMapper objectMapper) {
        this.persister = persister;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    // Khóa cột nội bộ — CHỈ thuộc bảng product_variant.
    private static final String C_VSKU = "vsku", C_OPTIONS = "options", C_SORT = "sort", C_ACTIVE = "active";

    private static final Map<String, String> HEADER_ALIASES = buildAliases();

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"skubienthe", "mabienthe", "variantsku", "skucode", "mavariant", "bienthesku", "sku", "masku"})
            m.put(a, C_VSKU);
        for (String a : new String[]{"thuoctinh", "phanloai", "options", "optionvalues", "tuychon", "bienthe", "thuoctinhbienthe", "giatri"})
            m.put(a, C_OPTIONS);
        for (String a : new String[]{"thutu", "sortorder", "sapxep"}) m.put(a, C_SORT);
        for (String a : new String[]{"active", "trangthai", "kichhoat", "hienthi"}) m.put(a, C_ACTIVE);
        return m;
    }

    private static final String[] TEMPLATE_HEADERS = {"sku_code", "option_values", "sort_order", "active"};

    // ── Public API ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public VariantImportResponse previewVariants(Long productId, MultipartFile file) {
        ProductEntity product = loadProduct(productId);
        ParseResult parsed = parseFile(file);
        List<ProductVariantEntity> existing = new ArrayList<>(product.getVariants());

        List<VariantImportRowResult> results = new ArrayList<>(parsed.orphanResults);
        int created = 0, updated = 0;
        for (ProductVariantImportPersister.VariantImportDraft d : parsed.drafts) {
            ProductVariantEntity match = matchVariant(existing, d);
            boolean exists = match != null;
            if (exists) updated++; else created++;
            results.add(VariantImportRowResult.builder()
                    .rowNumber(d.rowNumber).skuCode(d.skuCode).optionsLabel(optionsLabel(d.options))
                    .key(variantKey(d)).action(exists ? "UPDATE" : "CREATE").exists(exists)
                    .variantId(exists ? match.getId() : null)
                    .success(true)
                    .message(exists ? "Đã có — mặc định CẬP NHẬT" : "Mới — mặc định THÊM MỚI")
                    .build());
        }
        sortByRow(results);
        return VariantImportResponse.builder()
                .productId(productId)
                .totalVariants(parsed.drafts.size())
                .successCount(created + updated)
                .createdCount(created)
                .updatedCount(updated)
                .failureCount(parsed.orphanResults.size())
                .results(results)
                .build();
    }

    @Override
    public VariantImportResponse importVariants(Long productId, MultipartFile file, Map<String, String> actions) {
        List<ProductVariantEntity> existing = loadProductVariants(productId);
        ParseResult parsed = parseFile(file);

        List<VariantImportRowResult> results = new ArrayList<>(parsed.orphanResults);
        int created = 0, updated = 0, skipped = 0, failure = 0;
        for (ProductVariantImportPersister.VariantImportDraft d : parsed.drafts) {
            ProductVariantEntity match = matchVariant(existing, d);
            String key = variantKey(d);
            String chosen = actions != null ? actions.get(key) : null;
            boolean doUpdate;
            if ("CREATE".equalsIgnoreCase(chosen)) doUpdate = false;
            else if ("UPDATE".equalsIgnoreCase(chosen)) doUpdate = match != null;
            else doUpdate = match != null;

            try {
                ProductVariantImportPersister.PersistResult r;
                if (doUpdate && match != null) {
                    r = persister.update(productId, match.getId(), d);
                    if (r.skipped) skipped++; else updated++;
                } else {
                    r = persister.create(productId, d);
                    created++;
                }
                String action = r.skipped ? "SKIP" : (r.updated ? "UPDATE" : "CREATE");
                String msg = r.skipped ? "Bỏ qua (không thay đổi)"
                        : (r.updated ? "Cập nhật thành công" : "Tạo mới thành công");
                results.add(VariantImportRowResult.builder()
                        .rowNumber(d.rowNumber).skuCode(d.skuCode).optionsLabel(optionsLabel(d.options))
                        .key(key).action(action).exists(match != null)
                        .success(true).variantId(r.variantId)
                        .message(msg)
                        .build());
            } catch (Exception e) {
                failure++;
                results.add(VariantImportRowResult.builder()
                        .rowNumber(d.rowNumber).skuCode(d.skuCode).optionsLabel(optionsLabel(d.options))
                        .key(key).exists(match != null).success(false)
                        .message(rootMessage(e))
                        .build());
            }
        }
        failure += parsed.orphanResults.size();
        sortByRow(results);
        return VariantImportResponse.builder()
                .productId(productId)
                .totalVariants(parsed.drafts.size())
                .successCount(created + updated + skipped)
                .createdCount(created)
                .updatedCount(updated)
                .skippedCount(skipped)
                .failureCount(failure)
                .results(results)
                .build();
    }

    @Override
    public byte[] buildTemplateXlsx() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bien the");

            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor((short) 9);
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor((short) 23);
            headStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(TEMPLATE_HEADERS[i]);
                c.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 24 * 256);
            }

            String[][] examples = {
                    {"DELL-XPS13-I7", "CPU=i7;RAM=16GB;SSD=512GB", "0", "TRUE"},
                    {"DELL-XPS13-I5", "CPU=i5;RAM=8GB;SSD=256GB", "1", "TRUE"},
            };
            int r = 1;
            for (String[] ex : examples) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < ex.length; i++) row.createCell(i).setCellValue(ex[i]);
            }

            Sheet guide = wb.createSheet("Huong dan");
            String[] lines = {
                    "HƯỚNG DẪN ĐIỀN FILE IMPORT BIẾN THỂ (PHÂN LOẠI)",
                    "",
                    "File này nạp biến thể cho ĐÚNG sản phẩm bạn đang mở ở trang chi tiết.",
                    "",
                    "1. Mỗi DÒNG = một biến thể (phân loại) của sản phẩm.",
                    "2. Tiêu đề cột dùng đúng TÊN CỘT: sku_code, option_values, sort_order, active.",
                    "3. sku_code: mã SKU biến thể, DUY NHẤT toàn hệ thống (có thể để trống).",
                    "4. option_values (Thuộc tính): dạng 'Khóa=Giá trị', nhiều cặp ngăn bởi ';'. Ví dụ: CPU=i7;RAM=16GB",
                    "5. sort_order: thứ tự hiển thị (số nguyên, để trống sẽ tự xếp cuối).",
                    "6. active: TRUE/FALSE (hoặc 1/0, Có/Không). Để trống mặc định TRUE.",
                    "7. Khớp biến thể đã có: ưu tiên theo sku_code, nếu trống thì theo bộ option_values.",
                    "8. GIÁ & TỒN KHO của biến thể: nhập bằng chức năng quản lý giá / kho riêng.",
            };
            for (int i = 0; i < lines.length; i++) guide.createRow(i).createCell(0).setCellValue(lines[i]);
            guide.setColumnWidth(0, 110 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không tạo được file mẫu: " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ProductEntity loadProduct(Long productId) {
        return productRepository.findWithFullRelationsById(productId)
                .orElseThrow(() -> new CustomApiException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm (id=" + productId + ")"));
    }

    /** Biến thể được JOIN FETCH cùng sản phẩm nên không bị lazy khi đọc ngoài transaction. */
    private List<ProductVariantEntity> loadProductVariants(Long productId) {
        ProductEntity p = loadProduct(productId);
        return new ArrayList<>(p.getVariants());
    }

    private ProductVariantEntity matchVariant(List<ProductVariantEntity> existing,
                                              ProductVariantImportPersister.VariantImportDraft d) {
        if (!isBlank(d.skuCode)) {
            for (ProductVariantEntity v : existing) {
                if (v.getSkuCode() != null && v.getSkuCode().equalsIgnoreCase(d.skuCode.trim())) return v;
            }
            return null; // có sku_code nhưng chưa khớp -> biến thể mới
        }
        if (d.options != null && !d.options.isEmpty()) {
            for (ProductVariantEntity v : existing) {
                if (v.getOptionValues() != null && v.getOptionValues().equals(d.options)) return v;
            }
        }
        return null;
    }

    private String variantKey(ProductVariantImportPersister.VariantImportDraft d) {
        if (!isBlank(d.skuCode)) return "sku:" + normalizeHeader(d.skuCode);
        if (d.options != null && !d.options.isEmpty()) return "opt:" + normalizeHeader(optionsLabel(d.options));
        return "row:" + d.rowNumber;
    }

    private static String optionsLabel(Map<String, String> options) {
        if (options == null || options.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : options.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey());
            if (e.getValue() != null && !e.getValue().isEmpty()) sb.append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private static void sortByRow(List<VariantImportRowResult> results) {
        results.sort((a, b) -> Integer.compare(
                a.getRowNumber() != null ? a.getRowNumber() : 0,
                b.getRowNumber() != null ? b.getRowNumber() : 0));
    }

    // ── Parse ────────────────────────────────────────────────────────────────

    private static class ParseResult {
        List<ProductVariantImportPersister.VariantImportDraft> drafts = new ArrayList<>();
        List<VariantImportRowResult> orphanResults = new ArrayList<>();
    }

    private ParseResult parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "File rỗng hoặc chưa được chọn");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<String[]> rows;
        try {
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                rows = readSpreadsheet(file);
            } else if (name.endsWith(".csv") || name.endsWith(".txt") || name.isEmpty()) {
                rows = readDelimited(file);
            } else {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Định dạng không hỗ trợ. Chỉ nhận .xlsx, .csv hoặc .txt");
            }
        } catch (CustomApiException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Không đọc được file: " + e.getMessage());
        }

        if (rows.isEmpty()) throw new CustomApiException(HttpStatus.BAD_REQUEST, "File không có dữ liệu");

        Map<Integer, String> colMap = mapHeader(rows.get(0));
        if (!colMap.containsValue(C_VSKU) && !colMap.containsValue(C_OPTIONS)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'sku_code' hoặc 'option_values' ở dòng tiêu đề");
        }

        ParseResult parsed = new ParseResult();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int excelRow = i + 1;
            if (isRowEmpty(row)) continue;

            String vsku = get(row, colMap, C_VSKU);
            String opts = get(row, colMap, C_OPTIONS);
            if (isBlank(vsku) && isBlank(opts)) {
                parsed.orphanResults.add(VariantImportRowResult.builder()
                        .rowNumber(excelRow).success(false)
                        .message("Dòng thiếu cả sku_code lẫn option_values")
                        .build());
                continue;
            }
            ProductVariantImportPersister.VariantImportDraft d = new ProductVariantImportPersister.VariantImportDraft();
            d.rowNumber = excelRow;
            d.skuCode = nullIfBlank(vsku);
            d.options.putAll(parseOptions(opts));
            d.sortOrder = parseInt(get(row, colMap, C_SORT));
            d.active = parseBool(get(row, colMap, C_ACTIVE));
            parsed.drafts.add(d);
        }
        return parsed;
    }

    private Map<String, String> parseOptions(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (isBlank(raw)) return map;
        String s = raw.trim();
        if (s.startsWith("{")) {
            try {
                Map<?, ?> json = objectMapper.readValue(s, Map.class);
                for (Map.Entry<?, ?> e : json.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        map.put(String.valueOf(e.getKey()).trim(), String.valueOf(e.getValue()).trim());
                    }
                }
                return map;
            } catch (Exception ignore) {
                // rơi xuống parse key=value
            }
        }
        for (String pair : s.split("[;|\\n]")) {
            String pr = pair.trim();
            if (pr.isEmpty()) continue;
            int idx = indexOfAny(pr, '=', ':');
            if (idx > 0) map.put(pr.substring(0, idx).trim(), pr.substring(idx + 1).trim());
            else map.put(pr, "");
        }
        return map;
    }

    private static int indexOfAny(String s, char a, char b) {
        int i = s.indexOf(a);
        int j = s.indexOf(b);
        if (i < 0) return j;
        if (j < 0) return i;
        return Math.min(i, j);
    }

    // ── File reading ────────────────────────────────────────────────────────

    private List<String[]> readSpreadsheet(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return rows;
            int lastCol = 0;
            for (Row row : sheet) lastCol = Math.max(lastCol, row.getLastCellNum());
            for (Row row : sheet) {
                String[] cells = new String[lastCol < 0 ? 0 : lastCol];
                for (int c = 0; c < cells.length; c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    cells[c] = cellToString(cell);
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private String cellToString(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();
        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case NUMERIC:
                double d = cell.getNumericCellValue();
                return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            default:
                return "";
        }
    }

    private List<String[]> readDelimited(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '﻿') content = content.substring(1);
        char delim = detectDelimiter(content);
        return parseCsv(content, delim);
    }

    private char detectDelimiter(String content) {
        int nl = content.indexOf('\n');
        String first = nl >= 0 ? content.substring(0, nl) : content;
        int tab = count(first, '\t'), comma = count(first, ','), semi = count(first, ';');
        if (tab >= comma && tab >= semi && tab > 0) return '\t';
        if (semi > comma && semi > 0) return ';';
        return ',';
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    private List<String[]> parseCsv(String content, char delim) {
        List<String[]> rows = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == delim) {
                    cur.add(sb.toString());
                    sb.setLength(0);
                } else if (ch == '\n' || ch == '\r') {
                    if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') i++;
                    cur.add(sb.toString());
                    sb.setLength(0);
                    rows.add(cur.toArray(new String[0]));
                    cur = new ArrayList<>();
                } else {
                    sb.append(ch);
                }
            }
        }
        if (sb.length() > 0 || !cur.isEmpty()) {
            cur.add(sb.toString());
            rows.add(cur.toArray(new String[0]));
        }
        return rows;
    }

    // ── Header mapping & value parsing ─────────────────────────────────────────

    private Map<Integer, String> mapHeader(String[] header) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String norm = normalizeHeader(header[i]);
            String key = HEADER_ALIASES.get(norm);
            if (key != null && !map.containsValue(key)) map.put(i, key);
        }
        return map;
    }

    static String normalizeHeader(String raw) {
        if (raw == null) return "";
        String noAccent = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return noAccent.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String get(String[] row, Map<Integer, String> colMap, String key) {
        for (Map.Entry<Integer, String> e : colMap.entrySet()) {
            if (e.getValue().equals(key)) {
                int idx = e.getKey();
                return idx < row.length && row[idx] != null ? row[idx].trim() : "";
            }
        }
        return "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static boolean isRowEmpty(String[] row) {
        if (row == null) return true;
        for (String c : row) if (c != null && !c.trim().isEmpty()) return false;
        return true;
    }

    private Integer parseInt(String s) {
        if (isBlank(s)) return null;
        String t = s.trim().replaceAll("[^0-9-]", "");
        if (t.isEmpty() || t.equals("-")) return null;
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBool(String s) {
        if (isBlank(s)) return null;
        String t = normalizeHeader(s);
        if (t.isEmpty()) return null;
        switch (t) {
            case "true": case "1": case "yes": case "y": case "co": case "x": case "on": case "bat": case "active":
                return true;
            case "false": case "0": case "no": case "n": case "khong": case "off": case "tat":
                return false;
            default:
                return null;
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        String msg = cur.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getMessage();
        return msg != null ? msg : "Lỗi không xác định";
    }
}
