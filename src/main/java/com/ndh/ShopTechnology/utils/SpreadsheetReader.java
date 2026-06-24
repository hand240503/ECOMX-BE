package com.ndh.ShopTechnology.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Đọc file bảng tính (.xlsx/.xls) hoặc văn bản phân tách (.csv/.txt) thành List&lt;String[]&gt;.
 * Dùng chung cho các chức năng import (sản phẩm, thương hiệu, danh mục).
 */
public final class SpreadsheetReader {

    private SpreadsheetReader() {}

    public static List<String[]> read(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return readSpreadsheet(file);
        }
        return readDelimited(file);
    }

    private static List<String[]> readSpreadsheet(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return rows;
            int lastCol = 0;
            for (Row row : sheet) lastCol = Math.max(lastCol, row.getLastCellNum());
            for (Row row : sheet) {
                String[] cells = new String[lastCol < 0 ? 0 : lastCol];
                for (int c = 0; c < cells.length; c++) {
                    cells[c] = cellToString(row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private static String cellToString(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();
        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            default:
                return "";
        }
    }

    private static List<String[]> readDelimited(MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '﻿') content = content.substring(1);
        char delim = detectDelimiter(content);
        return parseCsv(content, delim);
    }

    private static char detectDelimiter(String content) {
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

    private static List<String[]> parseCsv(String content, char delim) {
        List<String[]> rows = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else inQuotes = false;
                } else sb.append(ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == delim) { cur.add(sb.toString()); sb.setLength(0); }
                else if (ch == '\n' || ch == '\r') {
                    if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') i++;
                    cur.add(sb.toString()); sb.setLength(0);
                    rows.add(cur.toArray(new String[0])); cur = new ArrayList<>();
                } else sb.append(ch);
            }
        }
        if (sb.length() > 0 || !cur.isEmpty()) { cur.add(sb.toString()); rows.add(cur.toArray(new String[0])); }
        return rows;
    }

    /** Chuẩn hóa tiêu đề: bỏ dấu, chữ thường, chỉ giữ a-z0-9. */
    public static String normalizeHeader(String raw) {
        if (raw == null) return "";
        String noAccent = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return noAccent.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public static boolean isRowEmpty(String[] row) {
        if (row == null) return true;
        for (String c : row) if (c != null && !c.trim().isEmpty()) return false;
        return true;
    }
}
