package com.ndh.ShopTechnology.services.importexport;

import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.utils.SpreadsheetReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiện ích dùng chung cho các chức năng import bằng Excel/CSV (tồn kho, chương trình KM…):
 * đọc file, ánh xạ tiêu đề, parse giá trị (số/ngày/bool) và tra cứu biến thể theo id hoặc sku_code.
 */
@Component
public class ImportSupport {

    private final ProductVariantRepository variantRepository;

    public ImportSupport(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd", "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm", "dd/MM/yyyy",
            "dd-MM-yyyy HH:mm", "dd-MM-yyyy", "MM/dd/yyyy"
    };

    // ── Đọc & ánh xạ tiêu đề ─────────────────────────────────────────────────

    /** Đọc file thành danh sách dòng; ném BAD_REQUEST nếu file rỗng/không đọc được. */
    public List<String[]> readRows(MultipartFile file) {
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
        return rows;
    }

    /** Ánh xạ chỉ số cột -> khóa nội bộ theo bảng alias (đã chuẩn hóa, bỏ dấu/hoa thường). */
    public Map<Integer, String> mapHeader(String[] header, Map<String, String> aliases) {
        Map<Integer, String> col = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String key = aliases.get(SpreadsheetReader.normalizeHeader(header[i]));
            if (key != null && !col.containsValue(key)) col.put(i, key);
        }
        return col;
    }

    public String get(String[] row, Map<Integer, String> col, String key) {
        for (Map.Entry<Integer, String> e : col.entrySet()) {
            if (e.getValue().equals(key)) {
                int idx = e.getKey();
                return idx < row.length && row[idx] != null ? row[idx].trim() : "";
            }
        }
        return "";
    }

    public boolean isRowEmpty(String[] row) {
        return SpreadsheetReader.isRowEmpty(row);
    }

    // ── Tra cứu biến thể ────────────────────────────────────────────────────

    /**
     * Tra biến thể theo id (ưu tiên) hoặc sku_code. Ném lỗi rõ ràng nếu thiếu/không thấy/trùng.
     */
    public ProductVariantEntity resolveVariant(String idStr, String skuCode, String role) {
        String prefix = (role == null || role.isBlank()) ? "" : role + ": ";
        Long id = parseLong(idStr);
        if (id != null) {
            return variantRepository.findWithProductAndPricesById(id)
                    .orElseThrow(() -> new CustomApiException(HttpStatus.NOT_FOUND,
                            prefix + "Không tìm thấy biến thể id=" + id));
        }
        if (!isBlank(skuCode)) {
            List<ProductVariantEntity> found = variantRepository.findBySkuCodeIgnoreCaseFetchProduct(skuCode.trim());
            if (found.isEmpty()) {
                throw new CustomApiException(HttpStatus.NOT_FOUND, prefix + "Không tìm thấy biến thể sku_code=" + skuCode);
            }
            if (found.size() > 1) {
                throw new CustomApiException(HttpStatus.CONFLICT, prefix + "sku_code trùng nhiều biến thể: " + skuCode);
            }
            return found.get(0);
        }
        throw new CustomApiException(HttpStatus.BAD_REQUEST, prefix + "Thiếu variant_id hoặc sku_code");
    }

    // ── Parse giá trị ────────────────────────────────────────────────────────

    public boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    public Long parseLong(String s) {
        Double d = parseNumber(s);
        return d == null ? null : d.longValue();
    }

    public Integer parseInt(String s) {
        Double d = parseNumber(s);
        return d == null ? null : (int) Math.round(d);
    }

    /** Parse số có thể có dấu phân tách nghìn (32.990.000 hoặc 32,990,000). */
    public Double parseNumber(String s) {
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
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean parseBool(String s) {
        if (isBlank(s)) return null;
        String t = SpreadsheetReader.normalizeHeader(s);
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

    /** Parse ngày theo nhiều định dạng phổ biến. Trả null nếu rỗng; ném lỗi nếu sai định dạng. */
    public Date parseDate(String s) {
        if (isBlank(s)) return null;
        String t = s.trim();
        for (String p : DATE_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p);
                sdf.setLenient(false);
                return sdf.parse(t);
            } catch (Exception ignore) {
                // thử định dạng tiếp theo
            }
        }
        throw new CustomApiException(HttpStatus.BAD_REQUEST,
                "Ngày không hợp lệ: '" + s + "'. Dùng dạng yyyy-MM-dd HH:mm hoặc dd/MM/yyyy.");
    }

    public static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        String msg = cur.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getMessage();
        return msg != null ? msg : "Lỗi không xác định";
    }

    // ── Sinh file Excel mẫu ──────────────────────────────────────────────────

    /** Tạo file .xlsx mẫu: 1 sheet dữ liệu (tiêu đề in đậm + dòng ví dụ) + 1 sheet hướng dẫn. */
    public static byte[] buildTemplate(String sheetName, String[] headers,
                                       String[][] examples, String[] guideLines) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet(sheetName);

            org.apache.poi.ss.usermodel.Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor((short) 9);
            org.apache.poi.ss.usermodel.CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor((short) 23);
            headStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            int r = 1;
            if (examples != null) {
                for (String[] ex : examples) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                    for (int i = 0; i < ex.length; i++) row.createCell(i).setCellValue(ex[i]);
                }
            }

            if (guideLines != null && guideLines.length > 0) {
                org.apache.poi.ss.usermodel.Sheet guide = wb.createSheet("Huong dan");
                for (int i = 0; i < guideLines.length; i++) {
                    guide.createRow(i).createCell(0).setCellValue(guideLines[i]);
                }
                guide.setColumnWidth(0, 120 * 256);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không tạo được file mẫu: " + e.getMessage());
        }
    }
}
