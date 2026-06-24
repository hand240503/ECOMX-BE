package com.ndh.ShopTechnology.services.product.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.dto.response.product.ProductImportResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductImportRowResult;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.services.product.ProductImportService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
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
public class ProductImportServiceImpl implements ProductImportService {

    private final ProductImportPersister persister;
    private final ObjectMapper objectMapper;
    private final com.ndh.ShopTechnology.repository.ProductRepository productRepository;

    public ProductImportServiceImpl(ProductImportPersister persister, ObjectMapper objectMapper,
                                    com.ndh.ShopTechnology.repository.ProductRepository productRepository) {
        this.persister = persister;
        this.objectMapper = objectMapper;
        this.productRepository = productRepository;
    }

    // Khóa cột nội bộ
    private static final String C_NAME = "name", C_CATEGORY = "category", C_BRAND = "brand",
            C_SKU = "sku", C_STATUS = "status", C_FEATURED = "featured", C_HOTSALE = "hotsale",
            C_DESC = "desc", C_LDESC = "ldesc", C_VSKU = "vsku", C_OPTIONS = "options",
            C_UNIT = "unit", C_PRICE = "price", C_OLDPRICE = "oldprice",
            C_SORT = "sort";

    private static final Map<String, String> HEADER_ALIASES = buildAliases();

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"tensanpham", "productname", "ten", "name", "tenhang", "tensp"}) m.put(a, C_NAME);
        for (String a : new String[]{"danhmuc", "category", "categoryid", "loai", "nhom", "categoryname", "categorycode", "nganhhang"}) m.put(a, C_CATEGORY);
        for (String a : new String[]{"thuonghieu", "brand", "brandid", "nhasanxuat", "hang", "nhanhieu"}) m.put(a, C_BRAND);
        for (String a : new String[]{"sku", "masku", "masanpham", "masp", "mahang"}) m.put(a, C_SKU);
        for (String a : new String[]{"trangthai", "status"}) m.put(a, C_STATUS);
        for (String a : new String[]{"noibat", "isfeatured", "featured", "spnoibat"}) m.put(a, C_FEATURED);
        for (String a : new String[]{"hotsale", "khuyenmai", "sale", "dangsale"}) m.put(a, C_HOTSALE);
        for (String a : new String[]{"mota", "description", "motangan"}) m.put(a, C_DESC);
        for (String a : new String[]{"motachitiet", "motadai", "ldescription", "longdescription", "chitiet"}) m.put(a, C_LDESC);
        for (String a : new String[]{"skubienthe", "mabienthe", "variantsku", "skucode", "mavariant", "bienthesku"}) m.put(a, C_VSKU);
        for (String a : new String[]{"thuoctinh", "phanloai", "options", "optionvalues", "tuychon", "bienthe", "thuoctinhbienthe"}) m.put(a, C_OPTIONS);
        for (String a : new String[]{"donvi", "unit", "unitid", "donvitinh", "dvt"}) m.put(a, C_UNIT);
        for (String a : new String[]{"giaban", "gia", "price", "currentvalue", "giahientai", "dongia"}) m.put(a, C_PRICE);
        for (String a : new String[]{"giacu", "oldvalue", "oldprice", "giagoc", "gianiemyet"}) m.put(a, C_OLDPRICE);
        for (String a : new String[]{"thutu", "sortorder", "sapxep"}) m.put(a, C_SORT);
        return m;
    }

    // Tiêu đề chuẩn cho file mẫu (đúng thứ tự)
    private static final String[] TEMPLATE_HEADERS = {
            "product_name", "category_id", "brand_id", "sku", "status", "is_featured", "hot_sale",
            "description", "sku_code", "option_values", "unit_id", "current_value", "old_value", "sort_order"
    };

    // ── Public API ───────────────────────────────────────────────────────────

    @Override
    public ProductImportResponse importProducts(MultipartFile file) {
        return importProducts(file, null);
    }

    /** Đọc & phân tích file thành danh sách draft sản phẩm + các dòng orphan lỗi (không ghi CSDL). */
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

        if (rows.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "File không có dữ liệu");
        }

        Map<Integer, String> colMap = mapHeader(rows.get(0));
        if (!colMap.containsValue(C_NAME)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'Tên sản phẩm' ở dòng tiêu đề");
        }

        List<ProductImportRowResult> results = new ArrayList<>();
        List<ProductImportDraft> drafts = new ArrayList<>();
        ProductImportDraft current = null;

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int excelRow = i + 1; // 1-based, kể cả tiêu đề
            if (isRowEmpty(row)) continue;

            String pname = get(row, colMap, C_NAME);
            if (!isBlank(pname)) {
                Long rowSku = parseLong(get(row, colMap, C_SKU));
                // Lặp lại tên sản phẩm ở dòng kế tiếp = cùng sản phẩm (thêm biến thể),
                // không tạo sản phẩm trùng. Nếu khác tên (hoặc khác SKU) thì là sản phẩm mới.
                boolean sameAsCurrent = current != null
                        && current.productName.equalsIgnoreCase(pname.trim())
                        && sameSku(current.sku, rowSku);
                if (sameAsCurrent) {
                    ProductImportDraft.VariantDraft v = buildVariant(row, colMap, excelRow);
                    current.variants.add(v);
                    addPrice(v, row, colMap, excelRow);
                    continue;
                }
                current = new ProductImportDraft();
                current.rowNumber = excelRow;
                current.productName = pname.trim();
                current.categoryRef = get(row, colMap, C_CATEGORY);
                current.brandRef = get(row, colMap, C_BRAND);
                current.sku = rowSku;
                current.status = parseInt(get(row, colMap, C_STATUS));
                current.isFeatured = parseBool(get(row, colMap, C_FEATURED));
                current.hotSale = parseBool(get(row, colMap, C_HOTSALE));
                current.description = nullIfBlank(get(row, colMap, C_DESC));
                current.longDescription = nullIfBlank(get(row, colMap, C_LDESC));
                drafts.add(current);
                ProductImportDraft.VariantDraft v = buildVariant(row, colMap, excelRow);
                current.variants.add(v);
                addPrice(v, row, colMap, excelRow);
            } else {
                if (current == null) {
                    results.add(ProductImportRowResult.builder()
                            .rowNumber(excelRow).success(false)
                            .message("Dòng thiếu tên sản phẩm và không thuộc sản phẩm nào phía trên")
                            .build());
                    continue;
                }
                String vsku = get(row, colMap, C_VSKU);
                String opts = get(row, colMap, C_OPTIONS);
                boolean newVariant = !isBlank(vsku) || !isBlank(opts);
                if (newVariant) {
                    ProductImportDraft.VariantDraft v = buildVariant(row, colMap, excelRow);
                    current.variants.add(v);
                    addPrice(v, row, colMap, excelRow);
                } else {
                    // Dòng chỉ thêm giá (đơn vị khác) cho biến thể hiện tại
                    ProductImportDraft.VariantDraft last =
                            current.variants.get(current.variants.size() - 1);
                    addPrice(last, row, colMap, excelRow);
                }
            }
        }

        ParseResult parsed = new ParseResult();
        parsed.drafts = drafts;
        parsed.orphanResults = results;
        return parsed;
    }

    /** Kết quả phân tích file. */
    private static class ParseResult {
        List<ProductImportDraft> drafts = new ArrayList<>();
        List<ProductImportRowResult> orphanResults = new ArrayList<>();
    }

    @Override
    public ProductImportResponse previewProducts(MultipartFile file) {
        ParseResult parsed = parseFile(file);
        List<ProductImportRowResult> results = new ArrayList<>(parsed.orphanResults);
        int created = 0, updated = 0;
        for (ProductImportDraft d : parsed.drafts) {
            ProductEntity existing = resolveExisting(d);
            boolean exists = existing != null;
            if (exists) updated++; else created++;
            results.add(ProductImportRowResult.builder()
                    .rowNumber(d.rowNumber).productName(d.productName)
                    .key(productKey(d)).action(exists ? "UPDATE" : "CREATE").exists(exists)
                    .productId(exists ? existing.getId() : null)
                    .variantCount(d.variants.size())
                    .success(true)
                    .message(exists ? "Đã tồn tại — mặc định CẬP NHẬT" : "Mới — mặc định THÊM MỚI")
                    .build());
        }
        sortByRow(results);
        return ProductImportResponse.builder()
                .totalProducts(parsed.drafts.size())
                .successCount(created + updated)
                .createdCount(created)
                .updatedCount(updated)
                .failureCount(parsed.orphanResults.size())
                .createdVariantCount(0)
                .results(results)
                .build();
    }

    @Override
    public ProductImportResponse importProducts(MultipartFile file, Map<String, String> actions) {
        ParseResult parsed = parseFile(file);
        List<ProductImportRowResult> results = new ArrayList<>(parsed.orphanResults);
        int created = 0, updated = 0, skipped = 0, failure = 0, variants = 0;
        for (ProductImportDraft d : parsed.drafts) {
            ProductEntity existing = resolveExisting(d);
            String key = productKey(d);
            String chosen = actions != null ? actions.get(key) : null;
            boolean doUpdate;
            if ("CREATE".equalsIgnoreCase(chosen)) {
                doUpdate = false;
            } else if ("UPDATE".equalsIgnoreCase(chosen)) {
                doUpdate = existing != null; // chọn cập nhật nhưng chưa tồn tại -> tạo mới
            } else {
                doUpdate = existing != null; // mặc định: tồn tại thì cập nhật
            }
            try {
                ProductImportPersister.PersistResult r;
                if (doUpdate && existing != null) {
                    r = persister.update(existing.getId(), d);
                    if (r.skipped) skipped++; else updated++;
                } else {
                    r = persister.persist(d);
                    created++;
                }
                variants += r.variantCount;
                String action = r.skipped ? "SKIP" : (r.updated ? "UPDATE" : "CREATE");
                String msg = r.skipped
                        ? "Bỏ qua (không thay đổi)"
                        : (r.updated ? "Cập nhật" : "Tạo mới") + " thành công (" + r.variantCount + " biến thể)";
                results.add(ProductImportRowResult.builder()
                        .rowNumber(d.rowNumber).productName(d.productName)
                        .key(key).action(action).exists(existing != null)
                        .success(true).productId(r.productId).variantCount(r.variantCount)
                        .message(msg)
                        .build());
            } catch (Exception e) {
                failure++;
                results.add(ProductImportRowResult.builder()
                        .rowNumber(d.rowNumber).productName(d.productName)
                        .key(key).exists(existing != null).success(false)
                        .message(rootMessage(e))
                        .build());
            }
        }
        failure += parsed.orphanResults.size();
        sortByRow(results);
        return ProductImportResponse.builder()
                .totalProducts(parsed.drafts.size())
                .successCount(created + updated + skipped)
                .createdCount(created)
                .updatedCount(updated)
                .skippedCount(skipped)
                .failureCount(failure)
                .createdVariantCount(variants)
                .results(results)
                .build();
    }

    /** Tìm sản phẩm đã tồn tại theo SKU (ưu tiên) rồi tới tên (không phân biệt hoa thường). */
    private ProductEntity resolveExisting(ProductImportDraft d) {
        ProductEntity e = null;
        if (d.sku != null) e = productRepository.findFirstBySku(d.sku).orElse(null);
        if (e == null && !isBlank(d.productName)) {
            e = productRepository.findFirstByProductNameIgnoreCase(d.productName.trim()).orElse(null);
        }
        return e;
    }

    /** Khóa định danh ổn định để FE gửi lại lựa chọn CREATE/UPDATE. */
    private String productKey(ProductImportDraft d) {
        if (d.sku != null) return "sku:" + d.sku;
        return "name:" + normalizeHeader(d.productName == null ? "" : d.productName);
    }

    private static void sortByRow(List<ProductImportRowResult> results) {
        results.sort((a, b) -> Integer.compare(
                a.getRowNumber() != null ? a.getRowNumber() : 0,
                b.getRowNumber() != null ? b.getRowNumber() : 0));
    }

    @Override
    public byte[] buildTemplateXlsx() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("San pham");

            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor((short) 9); // white
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor((short) 23); // dark blue-ish
            headStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(TEMPLATE_HEADERS[i]);
                c.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            String[][] examples = {
                    {"Laptop Dell XPS 13", "Laptop", "Dell", "100023", "1", "TRUE", "FALSE",
                            "Laptop cao cấp mỏng nhẹ", "DELL-XPS13-I7", "CPU=i7;RAM=16GB;SSD=512GB",
                            "Cái", "32990000", "35990000", "0"},
                    {"Laptop Dell XPS 13", "Laptop", "Dell", "100023", "1", "TRUE", "FALSE",
                            "Laptop cao cấp mỏng nhẹ", "DELL-XPS13-I5", "CPU=i5;RAM=8GB;SSD=256GB",
                            "Cái", "25990000", "28990000", "1"},
                    {"Chuột không dây Logitech", "Phụ kiện", "Logitech", "100050", "1", "FALSE", "TRUE",
                            "Chuột không dây bền bỉ", "CHUOT-LOGI-M1", "Màu=Đen",
                            "Cái", "290000", "350000", "0"},
                    {"Chuột không dây Logitech", "Phụ kiện", "Logitech", "100050", "1", "FALSE", "TRUE",
                            "Chuột không dây bền bỉ", "CHUOT-LOGI-M1-TRANG", "Màu=Trắng",
                            "Cái", "290000", "350000", "1"},
            };
            int r = 1;
            for (String[] ex : examples) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < ex.length; i++) row.createCell(i).setCellValue(ex[i]);
            }

            // Sheet hướng dẫn
            Sheet guide = wb.createSheet("Huong dan");
            String[] lines = {
                    "HƯỚNG DẪN ĐIỀN FILE IMPORT SẢN PHẨM",
                    "",
                    "1. Mỗi DÒNG = một biến thể. Nhiều dòng liên tiếp = nhiều biến thể của CÙNG một sản phẩm.",
                    "2. Các dòng biến thể của cùng một sản phẩm: LẶP LẠI cùng 'Tên sản phẩm' (và SKU) ở mỗi dòng,",
                    "   hoặc để TRỐNG cột 'Tên sản phẩm' ở các dòng sau — cả hai cách đều gom về một sản phẩm.",
                    "3. Tiêu đề cột dùng đúng TÊN CỘT trong CSDL: product_name, category_id, brand_id, sku, status,",
                    "   is_featured, hot_sale, description, sku_code, option_values, unit_id, current_value,",
                    "   old_value, sort_order.",
                    "4. category_id / brand_id / unit_id: điền TÊN hoặc code (hoặc id). Chưa có thì hệ thống tự tạo mới.",
                    "5. option_values (Thuộc tính): dạng 'Khóa=Giá trị', nhiều cặp ngăn bởi ';'. Ví dụ: CPU=i7;RAM=16GB",
                    "6. current_value (Giá bán) bắt buộc nếu có unit_id. old_value (Giá cũ) để trống nếu không khuyến mãi.",
                    "7. is_featured / hot_sale: TRUE/FALSE (hoặc 1/0, Có/Không).",
                    "8. status: 1 = hiển thị, 0 = ẩn (để trống mặc định 1).",
                    "9. Cùng biến thể nhiều đơn vị giá: thêm dòng để trống product_name, sku_code, option_values, chỉ điền unit_id + current_value.",
                    "10. TỒN KHO không nhập ở file này. Sau khi tạo sản phẩm, nhập số lượng tại chức năng 'Quản lý kho' (import tồn kho).",
            };
            for (int i = 0; i < lines.length; i++) {
                guide.createRow(i).createCell(0).setCellValue(lines[i]);
            }
            guide.setColumnWidth(0, 110 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không tạo được file mẫu: " + e.getMessage());
        }
    }

    // ── Row → draft helpers ───────────────────────────────────────────────────

    private ProductImportDraft.VariantDraft buildVariant(String[] row, Map<Integer, String> col, int excelRow) {
        ProductImportDraft.VariantDraft v = new ProductImportDraft.VariantDraft();
        v.rowNumber = excelRow;
        v.skuCode = nullIfBlank(get(row, col, C_VSKU));
        v.options.putAll(parseOptions(get(row, col, C_OPTIONS)));
        // Tồn kho KHÔNG nhập ở đây: import sản phẩm chỉ tạo product + biến thể + giá.
        // Số lượng tồn được nhập riêng ở chức năng "Quản lý kho" (import tồn kho).
        v.onHand = null;
        v.sortOrder = parseInt(get(row, col, C_SORT));
        v.active = null; // mặc định true ở tầng lưu
        return v;
    }

    private void addPrice(ProductImportDraft.VariantDraft v, String[] row, Map<Integer, String> col, int excelRow) {
        String unit = get(row, col, C_UNIT);
        String price = get(row, col, C_PRICE);
        String oldPrice = get(row, col, C_OLDPRICE);
        if (isBlank(unit) && isBlank(price) && isBlank(oldPrice)) return;
        ProductImportDraft.PriceDraft p = new ProductImportDraft.PriceDraft();
        p.rowNumber = excelRow;
        p.unitRef = nullIfBlank(unit);
        p.currentValue = parseNumber(price);
        p.oldValue = parseNumber(oldPrice);
        v.prices.add(p);
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
                // rơi xuống parse dạng key=value
            }
        }
        for (String pair : s.split("[;|\\n]")) {
            String pr = pair.trim();
            if (pr.isEmpty()) continue;
            int idx = indexOfAny(pr, '=', ':');
            if (idx > 0) {
                map.put(pr.substring(0, idx).trim(), pr.substring(idx + 1).trim());
            } else {
                map.put(pr, "");
            }
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

    // ── Đọc file ──────────────────────────────────────────────────────────────

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

    /** CSV parser hỗ trợ field bọc dấu ngoặc kép và xuống dòng trong ngoặc. */
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

    // ── Header mapping ─────────────────────────────────────────────────────────

    private Map<Integer, String> mapHeader(String[] header) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String norm = normalizeHeader(header[i]);
            String key = HEADER_ALIASES.get(norm);
            if (key != null && !map.containsValue(key)) {
                map.put(i, key);
            }
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

    // ── Value parsing ──────────────────────────────────────────────────────────

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

    /** Hai SKU coi là cùng sản phẩm nếu bằng nhau, hoặc ít nhất một bên để trống. */
    private static boolean sameSku(Long a, Long b) {
        if (a == null || b == null) return true;
        return a.equals(b);
    }

    private static String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static boolean isRowEmpty(String[] row) {
        if (row == null) return true;
        for (String c : row) if (c != null && !c.trim().isEmpty()) return false;
        return true;
    }

    private Long parseLong(String s) {
        Double d = parseNumber(s);
        return d == null ? null : d.longValue();
    }

    private Integer parseInt(String s) {
        Double d = parseNumber(s);
        return d == null ? null : (int) Math.round(d);
    }

    /** Parse số có thể có dấu phân tách nghìn (32.990.000 hoặc 32,990,000). */
    private Double parseNumber(String s) {
        if (isBlank(s)) return null;
        String t = s.trim().replaceAll("(?i)[a-zđ₫\\s]", "");
        if (t.isEmpty()) return null;
        boolean hasDot = t.indexOf('.') >= 0, hasComma = t.indexOf(',') >= 0;
        if (hasDot && hasComma) {
            int lastDot = t.lastIndexOf('.'), lastComma = t.lastIndexOf(',');
            char decimal = lastDot > lastComma ? '.' : ',';
            char thousand = decimal == '.' ? ',' : '.';
            t = t.replace(String.valueOf(thousand), "");
            t = t.replace(decimal, '.');
        } else if (hasComma) {
            // chỉ có dấu phẩy: nếu trông như nghìn (nhóm 3 số) thì bỏ, ngược lại là thập phân
            if (t.matches("-?\\d{1,3}(,\\d{3})+")) t = t.replace(",", "");
            else t = t.replace(',', '.');
        } else if (hasDot) {
            if (t.matches("-?\\d{1,3}(\\.\\d{3})+")) t = t.replace(".", "");
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBool(String s) {
        if (isBlank(s)) return null;
        String t = normalizeHeader(s); // bỏ dấu + lowercase + bỏ ký tự lạ
        if (t.isEmpty()) return null;
        switch (t) {
            case "true": case "1": case "yes": case "y": case "co": case "x": case "on": case "bat":
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
