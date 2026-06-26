package com.ndh.ShopTechnology.services.promotion.impl;

import com.ndh.ShopTechnology.dto.request.product.UpsertPriceChangeRequest;
import com.ndh.ShopTechnology.dto.request.promotion.UpsertPurchaseWithPurchaseRequest;
import com.ndh.ShopTechnology.dto.request.promotion.VolumePriceTierItemRequest;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportRowResult;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.ProductPriceChangeRepository;
import com.ndh.ShopTechnology.repository.PurchaseWithPurchaseOfferRepository;
import com.ndh.ShopTechnology.services.importexport.ImportSupport;
import com.ndh.ShopTechnology.services.product.ProductPriceChangeService;
import com.ndh.ShopTechnology.services.promotion.ProductVolumePriceTierService;
import com.ndh.ShopTechnology.services.promotion.PromotionImportService;
import com.ndh.ShopTechnology.services.promotion.PurchaseWithPurchaseOfferService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PromotionImportServiceImpl implements PromotionImportService {

    private final ImportSupport support;
    private final ProductPriceChangeService priceChangeService;
    private final PurchaseWithPurchaseOfferService pwpService;
    private final ProductVolumePriceTierService volumeTierService;
    private final ProductPriceChangeRepository priceChangeRepository;
    private final PurchaseWithPurchaseOfferRepository pwpRepository;

    public PromotionImportServiceImpl(ImportSupport support,
                                      ProductPriceChangeService priceChangeService,
                                      PurchaseWithPurchaseOfferService pwpService,
                                      ProductVolumePriceTierService volumeTierService,
                                      ProductPriceChangeRepository priceChangeRepository,
                                      PurchaseWithPurchaseOfferRepository pwpRepository) {
        this.support = support;
        this.priceChangeService = priceChangeService;
        this.pwpService = pwpService;
        this.volumeTierService = volumeTierService;
        this.priceChangeRepository = priceChangeRepository;
        this.pwpRepository = pwpRepository;
    }

    // PC

    private static final String PC_VID = "variantId", PC_SKU = "skuCode", PC_BASE = "basePrice",
            PC_SALE = "salePrice", PC_START = "startAt", PC_END = "endAt", PC_ENABLED = "enabled",
            PC_QTY_LIMIT = "quantityLimit", PC_MAX_CUST = "maxPerCustomer", PC_PAY = "paymentCode";

    private static final Map<String, String> PC_ALIASES = pcAliases();

    private static Map<String, String> pcAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"variantid", "idbienthe", "variant"}) m.put(a, PC_VID);
        for (String a : new String[]{"skucode", "sku", "skubienthe", "mabienthe"}) m.put(a, PC_SKU);
        for (String a : new String[]{"baseprice", "giagoc", "giaban", "gia"}) m.put(a, PC_BASE);
        for (String a : new String[]{"saleprice", "giakhuyenmai", "giasale", "giauudai"}) m.put(a, PC_SALE);
        for (String a : new String[]{"startat", "batdau", "tungay", "ngaybatdau", "thoigianbatdau"}) m.put(a, PC_START);
        for (String a : new String[]{"endat", "ketthuc", "denngay", "ngayketthuc", "thoigianketthuc"}) m.put(a, PC_END);
        for (String a : new String[]{"enabled", "kichhoat", "batdau2", "trangthai", "active"}) m.put(a, PC_ENABLED);
        for (String a : new String[]{"quantitylimit", "gioihansoluong", "soluonggioihan", "tonggioihan"}) m.put(a, PC_QTY_LIMIT);
        for (String a : new String[]{"maxpercustomer", "gioihanmoikhach", "toidamoikhach"}) m.put(a, PC_MAX_CUST);
        for (String a : new String[]{"paymentcode", "requiredpaymentmethodcode", "mathanhtoan", "phuongthucthanhtoan"}) m.put(a, PC_PAY);
        return m;
    }

    private static final String[] PC_HEADERS = {
            "variant_id", "sku_code", "base_price", "sale_price",
            "enabled", "quantity_limit", "max_per_customer", "required_payment_method_code"
    };

    @Override
    public CatalogImportResponse importPriceChanges(MultipartFile file, Date startAt, Date endAt) {
        requireStartAt(startAt);
        validateWindow(startAt, endAt);
        List<String[]> rows = support.readRows(file);
        Map<Integer, String> col = support.mapHeader(rows.get(0), PC_ALIASES);
        requireVariantColumn(col, PC_VID, PC_SKU);
        if (!col.containsValue(PC_BASE)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy cột 'base_price'");
        }
        CatalogImportResponse resp = newResponse();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (support.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String idStr = support.get(row, col, PC_VID);
            String sku = support.get(row, col, PC_SKU);
            String key = !support.isBlank(sku) ? sku : ("variant=" + idStr);
            try {
                ProductVariantEntity v = support.resolveVariant(idStr, sku, null);
                Double base = support.parseNumber(support.get(row, col, PC_BASE));
                if (base == null) throw new CustomApiException(HttpStatus.BAD_REQUEST, "Thiếu base_price");
                UpsertPriceChangeRequest req = new UpsertPriceChangeRequest();
                req.setBasePrice(base);
                req.setSalePrice(support.parseNumber(support.get(row, col, PC_SALE)));
                // Khung thời gian KHÔNG lấy từ file — dùng thời gian admin chọn sau review.
                req.setStartAt(startAt);
                req.setEndAt(endAt);
                req.setEnabled(support.parseBool(support.get(row, col, PC_ENABLED)));
                req.setQuantityLimit(support.parseInt(support.get(row, col, PC_QTY_LIMIT)));
                req.setMaxPerCustomer(support.parseInt(support.get(row, col, PC_MAX_CUST)));
                req.setRequiredPaymentMethodCode(support.nullIfBlank(support.get(row, col, PC_PAY)));

                // UPSERT theo khóa nghiệp vụ: (biến thể + start_at + end_at).
                List<ProductPriceChangeEntity> matches =
                        priceChangeRepository.findByVariantAndWindow(v.getId(), req.getStartAt(), req.getEndAt());
                if (!matches.isEmpty()) {
                    Long pcId = matches.get(0).getId();
                    priceChangeService.update(v.getProduct().getId(), v.getId(), pcId, req);
                    recordUpdated(resp, excelRow, key, pcId, "Đã cập nhật chương trình đổi giá (cùng khung thời gian)");
                } else {
                    Long id = priceChangeService.create(v.getProduct().getId(), v.getId(), req).getId();
                    recordCreated(resp, excelRow, key, id, "Đã tạo chương trình đổi giá");
                }
            } catch (Exception e) {
                recordFail(resp, excelRow, key, e);
            }
        }
        return resp;
    }

    @Override
    public byte[] buildPriceChangeTemplate() {
        String[][] examples = {
                {"", "DELL-XPS13-I7", "32990000", "29990000", "TRUE", "100", "2", ""},
                {"1024", "", "290000", "249000", "TRUE", "", "", "VNPAY"},
        };
        String[] guide = {
                "HƯỚNG DẪN IMPORT CHƯƠNG TRÌNH ĐỔI GIÁ (PC - PRICE CHANGE)",
                "",
                "1. Mỗi DÒNG = một chương trình đổi giá cho MỘT biến thể.",
                "2. Xác định biến thể: variant_id HOẶC sku_code (ưu tiên variant_id nếu điền cả hai).",
                "3. base_price (bắt buộc): giá gốc. sale_price: giá khuyến mãi (để trống nếu không có).",
                "4. KHUNG THỜI GIAN (bắt đầu/kết thúc) KHÔNG nằm trong file. Sau khi tải lên và xem review,",
                "   bạn chọn thời gian áp dụng — thời gian này áp cho TẤT CẢ dòng được chọn nhập.",
                "5. enabled: TRUE/FALSE (mặc định TRUE).",
                "6. quantity_limit: tổng số lượng giới hạn (để trống = không giới hạn).",
                "7. max_per_customer: giới hạn mỗi khách (để trống = không giới hạn).",
                "8. required_payment_method_code: mã phương thức thanh toán bắt buộc, ví dụ VNPAY (tùy chọn).",
                "9. UPSERT: nếu đã có chương trình CÙNG biến thể + CÙNG khung thời gian (bạn chọn) thì hệ thống",
                "   CẬP NHẬT, không tạo trùng. Khác khung thời gian = chương trình mới.",
        };
        return ImportSupport.buildTemplate("Doi gia", PC_HEADERS, examples, guide);
    }

    // PWP

    private static final String PWP_A_VID = "anchorVariantId", PWP_A_SKU = "anchorSku",
            PWP_C_VID = "companionVariantId", PWP_C_SKU = "companionSku", PWP_PRICE = "promoUnitPrice",
            PWP_MIN_ANCHOR = "minAnchorQuantity", PWP_PER_ANCHOR = "companionPerAnchor",
            PWP_MAX = "maxCompanionUnits", PWP_ENABLED = "enabled";

    private static final Map<String, String> PWP_ALIASES = pwpAliases();

    private static Map<String, String> pwpAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"anchorvariantid", "anchorvariant", "idbienthechinh", "bienthechinhid"}) m.put(a, PWP_A_VID);
        for (String a : new String[]{"anchorsku", "anchorskucode", "skuchinh", "mabienthechinh"}) m.put(a, PWP_A_SKU);
        for (String a : new String[]{"companionvariantid", "companionvariant", "idbienthekem", "bienthekemid"}) m.put(a, PWP_C_VID);
        for (String a : new String[]{"companionsku", "companionskucode", "skukem", "mabienthekem"}) m.put(a, PWP_C_SKU);
        for (String a : new String[]{"promounitprice", "giauudai", "giakem", "giapwp", "gia"}) m.put(a, PWP_PRICE);
        for (String a : new String[]{"minanchorquantity", "soluongchinhtoithieu", "minchinh"}) m.put(a, PWP_MIN_ANCHOR);
        for (String a : new String[]{"companionpromounitsperanchor", "companionperanchor", "sokemtrenchinh"}) m.put(a, PWP_PER_ANCHOR);
        for (String a : new String[]{"maxcompanionpromounits", "maxcompanionunits", "toidasokem"}) m.put(a, PWP_MAX);
        for (String a : new String[]{"enabled", "kichhoat", "trangthai", "active"}) m.put(a, PWP_ENABLED);
        return m;
    }

    private static final String[] PWP_HEADERS = {
            "anchor_variant_id", "anchor_sku_code", "companion_variant_id", "companion_sku_code",
            "promo_unit_price", "min_anchor_quantity", "companion_promo_units_per_anchor",
            "max_companion_promo_units", "enabled"
    };

    @Override
    public CatalogImportResponse importPurchaseWithPurchase(MultipartFile file, Date startAt, Date endAt) {
        requireStartAt(startAt);
        validateWindow(startAt, endAt);
        List<String[]> rows = support.readRows(file);
        Map<Integer, String> col = support.mapHeader(rows.get(0), PWP_ALIASES);
        if (!col.containsValue(PWP_A_VID) && !col.containsValue(PWP_A_SKU)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'anchor_variant_id' hoặc 'anchor_sku_code'");
        }
        if (!col.containsValue(PWP_C_VID) && !col.containsValue(PWP_C_SKU)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'companion_variant_id' hoặc 'companion_sku_code'");
        }
        CatalogImportResponse resp = newResponse();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (support.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String aId = support.get(row, col, PWP_A_VID), aSku = support.get(row, col, PWP_A_SKU);
            String cId = support.get(row, col, PWP_C_VID), cSku = support.get(row, col, PWP_C_SKU);
            String key = (!support.isBlank(aSku) ? aSku : "v" + aId) + " + " + (!support.isBlank(cSku) ? cSku : "v" + cId);
            try {
                ProductVariantEntity anchor = support.resolveVariant(aId, aSku, "Sản phẩm chính");
                ProductVariantEntity companion = support.resolveVariant(cId, cSku, "Sản phẩm kèm");
                Double price = support.parseNumber(support.get(row, col, PWP_PRICE));
                if (price == null) throw new CustomApiException(HttpStatus.BAD_REQUEST, "Thiếu promo_unit_price");
                UpsertPurchaseWithPurchaseRequest req = new UpsertPurchaseWithPurchaseRequest();
                req.setAnchorProductId(anchor.getProduct().getId());
                req.setAnchorVariantId(anchor.getId());
                req.setCompanionProductId(companion.getProduct().getId());
                req.setCompanionVariantId(companion.getId());
                req.setPromoUnitPrice(price);
                req.setMinAnchorQuantity(support.parseInt(support.get(row, col, PWP_MIN_ANCHOR)));
                req.setCompanionPromoUnitsPerAnchor(support.parseInt(support.get(row, col, PWP_PER_ANCHOR)));
                req.setMaxCompanionPromoUnits(support.parseInt(support.get(row, col, PWP_MAX)));
                req.setEnabled(support.parseBool(support.get(row, col, PWP_ENABLED)));
                // Khung thời gian do admin chọn sau review, áp cho tất cả dòng.
                req.setStartAt(startAt);
                req.setEndAt(endAt);

                // UPSERT theo khóa nghiệp vụ: companion_variant_id (đã unique trong DB).
                PurchaseWithPurchaseOfferEntity existing =
                        pwpRepository.findByCompanionVariant_Id(companion.getId()).orElse(null);
                if (existing != null) {
                    pwpService.update(existing.getId(), req);
                    recordUpdated(resp, excelRow, key, existing.getId(), "Đã cập nhật chương trình mua kèm");
                } else {
                    Long id = pwpService.create(req).getId();
                    recordCreated(resp, excelRow, key, id, "Đã tạo chương trình mua kèm");
                }
            } catch (Exception e) {
                recordFail(resp, excelRow, key, e);
            }
        }
        return resp;
    }

    @Override
    public byte[] buildPurchaseWithPurchaseTemplate() {
        String[][] examples = {
                {"", "DELL-XPS13-I7", "", "CHUOT-LOGI-M1", "99000", "1", "1", "2", "TRUE"},
                {"1024", "", "2048", "", "150000", "1", "1", "", "TRUE"},
        };
        String[] guide = {
                "HƯỚNG DẪN IMPORT MUA KÈM (PWP - PURCHASE WITH PURCHASE)",
                "",
                "1. Mỗi DÒNG = một ưu đãi: mua sản phẩm CHÍNH (anchor) thì được mua sản phẩm KÈM (companion) giá ưu đãi.",
                "2. Sản phẩm chính: anchor_variant_id HOẶC anchor_sku_code.",
                "3. Sản phẩm kèm:  companion_variant_id HOẶC companion_sku_code.",
                "   (Ưu tiên *_variant_id nếu điền cả id và sku.)",
                "4. promo_unit_price (bắt buộc): giá ưu đãi của sản phẩm kèm.",
                "5. min_anchor_quantity: số lượng sản phẩm chính tối thiểu (mặc định 1).",
                "6. companion_promo_units_per_anchor: số sản phẩm kèm ưu đãi cho mỗi sản phẩm chính (mặc định 1).",
                "7. max_companion_promo_units: tổng số sản phẩm kèm ưu đãi tối đa (để trống = không giới hạn).",
                "8. enabled: TRUE/FALSE (mặc định TRUE). Lưu ý: mỗi biến thể kèm chỉ thuộc 1 chương trình PWP.",
                "9. KHUNG THỜI GIAN (bắt đầu/kết thúc) KHÔNG nằm trong file. Sau khi tải lên và xem review,",
                "   bạn chọn thời gian áp dụng — áp cho TẤT CẢ dòng được chọn nhập.",
                "10. UPSERT: nếu biến thể KÈM (companion) đã có chương trình thì hệ thống CẬP NHẬT chương trình đó,",
                "    không tạo trùng (vì companion là duy nhất).",
        };
        return ImportSupport.buildTemplate("Mua kem", PWP_HEADERS, examples, guide);
    }

    // Mix & Match (Volume tier)

    private static final String VT_VID = "variantId", VT_SKU = "skuCode",
            VT_MIN = "minQuantity", VT_PRICE = "unitPrice", VT_ENABLED = "enabled";

    private static final Map<String, String> VT_ALIASES = vtAliases();

    private static Map<String, String> vtAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"variantid", "idbienthe", "variant"}) m.put(a, VT_VID);
        for (String a : new String[]{"skucode", "sku", "skubienthe", "mabienthe"}) m.put(a, VT_SKU);
        for (String a : new String[]{"minquantity", "soluongtoithieu", "tu", "mucsoluong", "minsl"}) m.put(a, VT_MIN);
        for (String a : new String[]{"unitprice", "giadonvi", "gia", "giaban"}) m.put(a, VT_PRICE);
        for (String a : new String[]{"enabled", "kichhoat", "trangthai", "active"}) m.put(a, VT_ENABLED);
        return m;
    }

    private static final String[] VT_HEADERS = {"variant_id", "sku_code", "min_quantity", "unit_price", "enabled"};

    @Override
    public CatalogImportResponse importVolumeTiers(MultipartFile file, Date startAt, Date endAt) {
        requireStartAt(startAt);
        validateWindow(startAt, endAt);
        List<String[]> rows = support.readRows(file);
        Map<Integer, String> col = support.mapHeader(rows.get(0), VT_ALIASES);
        requireVariantColumn(col, VT_VID, VT_SKU);
        if (!col.containsValue(VT_MIN) || !col.containsValue(VT_PRICE)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'min_quantity' và/hoặc 'unit_price'");
        }

        // Gom các dòng theo biến thể (mỗi biến thể có nhiều mốc số lượng).
        Map<Long, VariantTierGroup> groups = new LinkedHashMap<>();
        CatalogImportResponse resp = newResponse();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (support.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String idStr = support.get(row, col, VT_VID);
            String sku = support.get(row, col, VT_SKU);
            String key = !support.isBlank(sku) ? sku : ("variant=" + idStr);
            try {
                ProductVariantEntity v = support.resolveVariant(idStr, sku, null);
                Integer minQty = support.parseInt(support.get(row, col, VT_MIN));
                Double unitPrice = support.parseNumber(support.get(row, col, VT_PRICE));
                if (minQty == null) throw new CustomApiException(HttpStatus.BAD_REQUEST, "Thiếu min_quantity");
                if (unitPrice == null) throw new CustomApiException(HttpStatus.BAD_REQUEST, "Thiếu unit_price");
                VariantTierGroup g = groups.computeIfAbsent(v.getId(), x -> {
                    VariantTierGroup ng = new VariantTierGroup();
                    ng.productId = v.getProduct().getId();
                    ng.variantId = v.getId();
                    ng.key = key;
                    ng.firstRow = excelRow;
                    return ng;
                });
                VolumePriceTierItemRequest item = new VolumePriceTierItemRequest();
                item.setMinQuantity(minQty);
                item.setUnitPrice(unitPrice);
                item.setEnabled(support.parseBool(support.get(row, col, VT_ENABLED)));
                // Khung thời gian do admin chọn sau review, áp cho mọi mốc trong file.
                item.setStartAt(startAt);
                item.setEndAt(endAt);
                g.tiers.put(minQty, item); // dòng sau ghi đè dòng trước nếu trùng min_quantity
            } catch (Exception e) {
                recordFail(resp, excelRow, key, e);
            }
        }

        // Mỗi biến thể: GỘP với mốc đang có (chỉ thêm/sửa, không xóa mốc cũ) rồi lưu.
        for (VariantTierGroup g : groups.values()) {
            try {
                Map<Integer, VolumePriceTierItemRequest> merged = new LinkedHashMap<>();
                for (VolumePriceTierResponse existing :
                        volumeTierService.listByVariant(g.productId, g.variantId)) {
                    VolumePriceTierItemRequest it = new VolumePriceTierItemRequest();
                    it.setMinQuantity(existing.getMinQuantity());
                    it.setUnitPrice(existing.getUnitPrice());
                    it.setEnabled(existing.getEnabled());
                    it.setStartAt(existing.getStartAt());
                    it.setEndAt(existing.getEndAt());
                    merged.put(existing.getMinQuantity(), it);
                }
                merged.putAll(g.tiers); // mốc trong file ghi đè mốc cũ cùng min_quantity
                List<VolumePriceTierItemRequest> finalTiers = new ArrayList<>(merged.values());
                volumeTierService.replaceTiers(g.productId, g.variantId, finalTiers);
                recordCreated(resp, g.firstRow, g.key, g.variantId,
                        "Đã lưu " + g.tiers.size() + " mốc giá (tổng " + finalTiers.size() + " mốc)");
            } catch (Exception e) {
                recordFail(resp, g.firstRow, g.key, e);
            }
        }
        return resp;
    }

    @Override
    public byte[] buildVolumeTierTemplate() {
        String[][] examples = {
                {"", "DELL-XPS13-I7", "2", "31990000", "TRUE"},
                {"", "DELL-XPS13-I7", "5", "30990000", "TRUE"},
                {"1024", "", "3", "280000", "TRUE"},
        };
        String[] guide = {
                "HƯỚNG DẪN IMPORT GIÁ THEO SỐ LƯỢNG (MIX & MATCH / VOLUME PRICE TIER)",
                "",
                "1. Mỗi DÒNG = một MỐC giá theo số lượng cho một biến thể. Một biến thể có thể có NHIỀU dòng (nhiều mốc).",
                "2. Xác định biến thể: variant_id HOẶC sku_code (ưu tiên variant_id).",
                "3. min_quantity: số lượng tối thiểu để áp mốc giá này (số nguyên >= 1).",
                "4. unit_price: đơn giá khi đạt mốc số lượng đó.",
                "5. enabled: TRUE/FALSE (mặc định TRUE).",
                "6. KHUNG THỜI GIAN (bắt đầu/kết thúc) KHÔNG nằm trong file. Sau khi tải lên và xem review,",
                "   bạn chọn thời gian áp dụng — áp cho TẤT CẢ mốc được nhập.",
                "7. Import sẽ THÊM/CẬP NHẬT các mốc (gộp với mốc đang có; mốc cùng min_quantity sẽ bị ghi đè).",
                "   Không xóa mốc cũ qua import — để xóa hãy dùng giao diện quản lý Mix & Match.",
        };
        return ImportSupport.buildTemplate("Mix and Match", VT_HEADERS, examples, guide);
    }

    // helpers

    private static class VariantTierGroup {
        Long productId;
        Long variantId;
        String key;
        int firstRow;
        final Map<Integer, VolumePriceTierItemRequest> tiers = new LinkedHashMap<>();
    }

    /** start_at là bắt buộc — admin phải chọn thời điểm bắt đầu sau khi review. */
    private static void requireStartAt(Date startAt) {
        if (startAt == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Thiếu thời điểm bắt đầu. Hãy chọn thời gian áp dụng sau khi xem review.");
        }
    }

    /** end_at (nếu có) phải >= start_at. */
    private static void validateWindow(Date startAt, Date endAt) {
        if (startAt != null && endAt != null && endAt.before(startAt)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Thời điểm kết thúc phải sau hoặc bằng thời điểm bắt đầu.");
        }
    }

    private void requireVariantColumn(Map<Integer, String> col, String idKey, String skuKey) {
        if (!col.containsValue(idKey) && !col.containsValue(skuKey)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột 'variant_id' hoặc 'sku_code' ở dòng tiêu đề");
        }
    }

    private static CatalogImportResponse newResponse() {
        return CatalogImportResponse.builder()
                .totalRows(0).createdCount(0).updatedCount(0).failureCount(0)
                .results(new ArrayList<>()).build();
    }

    private void recordCreated(CatalogImportResponse resp, int rowNum, String key, Long id, String msg) {
        resp.setTotalRows(resp.getTotalRows() + 1);
        resp.setCreatedCount(resp.getCreatedCount() + 1);
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action("CREATED").success(true).id(id).message(msg)
                .build());
    }

    private void recordUpdated(CatalogImportResponse resp, int rowNum, String key, Long id, String msg) {
        resp.setTotalRows(resp.getTotalRows() + 1);
        resp.setUpdatedCount(resp.getUpdatedCount() + 1);
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action("UPDATED").success(true).id(id).message(msg)
                .build());
    }

    private void recordFail(CatalogImportResponse resp, int rowNum, String key, Throwable e) {
        resp.setTotalRows(resp.getTotalRows() + 1);
        resp.setFailureCount(resp.getFailureCount() + 1);
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action("FAILED").success(false)
                .message(ImportSupport.rootMessage(e))
                .build());
    }
}
