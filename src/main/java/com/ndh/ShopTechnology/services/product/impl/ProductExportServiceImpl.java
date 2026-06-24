package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.PriceRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.services.product.ProductExportService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductExportServiceImpl implements ProductExportService {

    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ProductExportServiceImpl(ProductRepository productRepository, PriceRepository priceRepository,
                                    BrandRepository brandRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    // Tiêu đề = tên cột CSDL (gộp các bảng products / category / brands / product_variant / price / unit)
    private static final String[] HEADERS = {
            "product_id", "product_name", "category_id", "category_code", "category_name",
            "brand_id", "brand_code", "brand_name", "sku", "status", "is_featured", "hot_sale",
            "sold_count", "tag", "description", "l_description",
            "variant_id", "sku_code", "option_values", "active", "sort_order",
            "on_hand", "reserved", "available",
            "price_id", "unit_id", "unit_name", "unit_ratio", "current_value", "old_value", "display_name"
    };

    @Override
    @Transactional(readOnly = true)
    public byte[] exportProductsXlsx() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("San pham");

            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor((short) 9); // white
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor((short) 23);
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }

            List<ProductEntity> products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

            // Nạp tất cả biến thể để gom price theo lô (tránh N+1 cho bảng giá)
            List<ProductVariantEntity> allVariants = new ArrayList<>();
            for (ProductEntity p : products) {
                if (p.getVariants() != null) allVariants.addAll(p.getVariants());
            }
            List<Long> variantIds = allVariants.stream()
                    .map(ProductVariantEntity::getId).filter(java.util.Objects::nonNull).collect(Collectors.toList());

            Map<Long, List<PriceEntity>> pricesByVariant = new LinkedHashMap<>();
            if (!variantIds.isEmpty()) {
                for (PriceEntity pr : priceRepository.findAllWithUnitByVariantIdIn(variantIds)) {
                    Long vid = pr.getVariant() != null ? pr.getVariant().getId() : null;
                    if (vid != null) pricesByVariant.computeIfAbsent(vid, k -> new ArrayList<>()).add(pr);
                }
            }

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            int r = 1;
            for (ProductEntity p : products) {
                CategoryEntity cat = p.getCategory();
                BrandEntity brand = p.getBrand();
                List<ProductVariantEntity> variants = p.getVariants() != null ? p.getVariants() : List.of();

                if (variants.isEmpty()) {
                    r = writeRow(sheet, r, df, p, cat, brand, null, null);
                    continue;
                }
                for (ProductVariantEntity v : variants) {
                    List<PriceEntity> prices = pricesByVariant.get(v.getId());
                    if (prices == null || prices.isEmpty()) {
                        r = writeRow(sheet, r, df, p, cat, brand, v, null);
                    } else {
                        for (PriceEntity pr : prices) {
                            r = writeRow(sheet, r, df, p, cat, brand, v, pr);
                        }
                    }
                }
            }

            sheet.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không xuất được file Excel: " + e.getMessage());
        }
    }

    // Tiêu đề cho file "sản phẩm chưa hoàn thiện" — gồm thông tin SP + cột biến thể/giá để trống điền thêm.
    private static final String[] INCOMPLETE_HEADERS = {
            "product_id", "product_name", "category_code", "category_name", "brand_code", "brand_name",
            "sku", "status", "is_featured", "hot_sale", "description", "missing",
            "sku_code", "option_values", "unit_id", "current_value", "old_value", "sort_order"
    };

    @Override
    @Transactional(readOnly = true)
    public byte[] exportIncompleteProductsXlsx() {
        List<ProductEntity> products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        // Gom giá theo biến thể để biết biến thể nào CHƯA có giá (tránh N+1).
        List<Long> variantIds = new ArrayList<>();
        for (ProductEntity p : products) {
            if (p.getVariants() != null) {
                for (ProductVariantEntity v : p.getVariants()) {
                    if (v.getId() != null) variantIds.add(v.getId());
                }
            }
        }
        java.util.Set<Long> pricedVariantIds = new java.util.HashSet<>();
        if (!variantIds.isEmpty()) {
            for (PriceEntity pr : priceRepository.findAllWithUnitByVariantIdIn(variantIds)) {
                if (pr.getVariant() != null && pr.getVariant().getId() != null) {
                    pricedVariantIds.add(pr.getVariant().getId());
                }
            }
        }

        List<Object[]> rows = new ArrayList<>();
        for (ProductEntity p : products) {
            CategoryEntity cat = p.getCategory();
            BrandEntity brand = p.getBrand();
            List<ProductVariantEntity> variants = p.getVariants() != null ? p.getVariants() : List.of();

            if (variants.isEmpty()) {
                // Chưa có biến thể nào.
                rows.add(incompleteRow(p, cat, brand, "NO_VARIANT", null));
            } else {
                // Có biến thể nhưng TẤT CẢ đều chưa có giá → sản phẩm chưa bán được.
                List<ProductVariantEntity> unpriced = variants.stream()
                        .filter(v -> v.getId() == null || !pricedVariantIds.contains(v.getId()))
                        .collect(Collectors.toList());
                if (unpriced.size() == variants.size()) {
                    for (ProductVariantEntity v : unpriced) {
                        rows.add(incompleteRow(p, cat, brand, "NO_PRICE", v));
                    }
                }
                // Nếu chỉ một phần biến thể thiếu giá thì sản phẩm vẫn bán được → bỏ qua.
            }
        }

        return buildSheet("SP chua hoan thien", INCOMPLETE_HEADERS, rows);
    }

    /** Một dòng cho file sản phẩm chưa hoàn thiện (cột giá/đơn vị để trống cho người dùng điền). */
    private Object[] incompleteRow(ProductEntity p, CategoryEntity cat, BrandEntity brand,
                                   String missing, ProductVariantEntity v) {
        return new Object[]{
                p.getId(),
                p.getProductName(),
                cat != null ? cat.getCode() : null,
                cat != null ? cat.getName() : null,
                brand != null ? brand.getCode() : null,
                brand != null ? brand.getName() : null,
                p.getSku(),
                p.getStatus(),
                p.getIsFeatured(),
                p.getHotSale(),
                p.getDescription(),
                missing,
                v != null ? v.getSkuCode() : null,
                v != null ? formatOptions(v.getOptionValues()) : null,
                null, // unit_id  (để trống điền)
                null, // current_value
                null, // old_value
                v != null ? v.getSortOrder() : null
        };
    }

    /** Map option -> chuỗi "k=v;k=v" (đúng định dạng cột option_values khi import). */
    private static String formatOptions(Map<String, String> opts) {
        if (opts == null || opts.isEmpty()) return null;
        return opts.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
    }

    private static final String[] BRAND_HEADERS = {
            "id", "code", "name", "status"
    };

    private static final String[] CATEGORY_HEADERS = {
            "id", "code", "name", "status", "parent_id", "parent_name"
    };

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBrandsXlsx() {
        List<Object[]> rows = new ArrayList<>();
        for (BrandEntity b : brandRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))) {
            rows.add(new Object[]{ b.getId(), b.getCode(), b.getName(), b.getStatus() });
        }
        return buildSheet("Brands", BRAND_HEADERS, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCategoriesXlsx() {
        List<Object[]> rows = new ArrayList<>();
        for (CategoryEntity cat : categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))) {
            CategoryEntity parent = cat.getParent();
            rows.add(new Object[]{
                    cat.getId(), cat.getCode(), cat.getName(), cat.getStatus(),
                    parent != null ? parent.getId() : null,
                    parent != null ? parent.getName() : null
            });
        }
        return buildSheet("Categories", CATEGORY_HEADERS, rows);
    }

    /** Tạo workbook 1 sheet từ tiêu đề + danh sách dòng (Number/Boolean/String/null). */
    private byte[] buildSheet(String sheetName, String[] headers, List<Object[]> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor((short) 9);
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor((short) 23);
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            int r = 1;
            for (Object[] vals : rows) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < vals.length; i++) {
                    Object v = vals[i];
                    if (v == null) continue;
                    if (v instanceof Number) row.createCell(i).setCellValue(((Number) v).doubleValue());
                    else if (v instanceof Boolean) row.createCell(i).setCellValue((Boolean) v);
                    else row.createCell(i).setCellValue(String.valueOf(v));
                }
            }

            sheet.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không xuất được file Excel: " + e.getMessage());
        }
    }

    private int writeRow(Sheet sheet, int rowIdx, SimpleDateFormat df,
                         ProductEntity p, CategoryEntity cat, BrandEntity brand,
                         ProductVariantEntity v, PriceEntity pr) {
        Row row = sheet.createRow(rowIdx);
        int c = 0;
        // products
        setNum(row, c++, p.getId());
        setStr(row, c++, p.getProductName());
        setNum(row, c++, cat != null ? cat.getId() : null);
        setStr(row, c++, cat != null ? cat.getCode() : null);
        setStr(row, c++, cat != null ? cat.getName() : null);
        setNum(row, c++, brand != null ? brand.getId() : null);
        setStr(row, c++, brand != null ? brand.getCode() : null);
        setStr(row, c++, brand != null ? brand.getName() : null);
        setNum(row, c++, p.getSku());
        setNum(row, c++, p.getStatus());
        setBool(row, c++, p.getIsFeatured());
        setBool(row, c++, p.getHotSale());
        setNum(row, c++, p.getSoldCount());
        setStr(row, c++, p.getTag());
        setStr(row, c++, p.getDescription());
        setStr(row, c++, p.getLDescription());
        // variant
        setNum(row, c++, v != null ? v.getId() : null);
        setStr(row, c++, v != null ? v.getSkuCode() : null);
        setStr(row, c++, v != null ? optionsToString(v.getOptionValues()) : null);
        setBool(row, c++, v != null ? v.getActive() : null);
        setNum(row, c++, v != null ? v.getSortOrder() : null);
        setNum(row, c++, v != null ? v.getOnHand() : null);
        setNum(row, c++, v != null ? v.getReserved() : null);
        setNum(row, c++, v != null ? (Integer) v.getAvailable() : null);
        // price + unit
        UnitEntity unit = pr != null ? pr.getUnit() : null;
        setNum(row, c++, pr != null ? pr.getId() : null);
        setNum(row, c++, unit != null ? unit.getId() : null);
        setStr(row, c++, unit != null ? unit.getNameUnit() : null);
        setNum(row, c++, unit != null ? unit.getRatio() : null);
        setNum(row, c++, pr != null ? pr.getCurrentValue() : null);
        setNum(row, c++, pr != null ? pr.getOldValue() : null);
        setStr(row, c++, pr != null ? pr.getDisplayName() : null);
        return rowIdx + 1;
    }

    // ── cell helpers ──────────────────────────────────────────────────────────

    private static void setStr(Row row, int col, String val) {
        if (val != null) row.createCell(col).setCellValue(val);
    }

    private static void setNum(Row row, int col, Number val) {
        if (val != null) row.createCell(col).setCellValue(val.doubleValue());
    }

    private static void setBool(Row row, int col, Boolean val) {
        if (val != null) row.createCell(col).setCellValue(val);
    }

    private static String fmtDate(SimpleDateFormat df, Date d) {
        return d != null ? df.format(d) : null;
    }

    /** Map option_values -> "k=v;k=v" (đồng dạng định dạng import). */
    private static String optionsToString(Map<String, String> opts) {
        if (opts == null || opts.isEmpty()) return null;
        return opts.entrySet().stream()
                .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue() : ""))
                .collect(Collectors.joining(";"));
    }
}
