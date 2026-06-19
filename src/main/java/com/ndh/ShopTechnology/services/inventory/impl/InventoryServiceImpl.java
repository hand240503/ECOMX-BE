package com.ndh.ShopTechnology.services.inventory.impl;

import com.ndh.ShopTechnology.dto.response.inventory.InventoryLedgerResponse;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryStockResponse;
import com.ndh.ShopTechnology.entities.inventory.InventoryLedgerEntity;
import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.enums.inventory.InventoryMovementType;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.InventoryLedgerRepository;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.services.inventory.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository variantRepository;
    private final InventoryLedgerRepository ledgerRepository;

    public InventoryServiceImpl(ProductVariantRepository variantRepository,
                                InventoryLedgerRepository ledgerRepository) {
        this.variantRepository = variantRepository;
        this.ledgerRepository = ledgerRepository;
    }

    // ============================ LUỒNG ĐƠN HÀNG ============================

    @Override
    @Transactional
    public void reserveForOrder(List<OrderDetailEntity> details, boolean throwIfInsufficient) {
        if (details == null) return;
        for (OrderDetailEntity d : details) {
            int qty = qtyOf(d);
            if (qty <= 0) continue;
            ProductVariantEntity variant = resolveVariant(d);
            if (variant == null) continue;

            // Idempotency: đã giữ cho dòng đơn này rồi thì bỏ qua.
            if (ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.RESERVE)) {
                continue;
            }

            int updated = variantRepository.reserveStock(variant.getId(), qty);
            if (updated == 0) {
                if (throwIfInsufficient) {
                    int available = currentAvailable(variant.getId());
                    throw new CustomApiException(HttpStatus.CONFLICT,
                            "Không đủ hàng trong kho cho sản phẩm '" + productName(variant)
                                    + "' (SKU " + skuOf(variant) + "). Cần " + qty
                                    + ", còn lại " + available + ".");
                }
                // Đơn đã thanh toán: vẫn giữ chỗ (cho phép oversell), ghi cảnh báo để admin xử lý.
                variantRepository.forceReserve(variant.getId(), qty);
                log.warn("reserveForOrder: OVERSELL — giữ chỗ vượt tồn cho đơn đã thanh toán. variant={} orderDetail={} qty={}",
                        variant.getId(), d.getId(), qty);
            }
            writeLedger(variant, d, InventoryMovementType.RESERVE, qty, "Giữ hàng khi đặt đơn");
        }
    }

    @Override
    @Transactional
    public void releaseForOrder(List<OrderDetailEntity> details) {
        if (details == null) return;
        for (OrderDetailEntity d : details) {
            int qty = qtyOf(d);
            if (qty <= 0) continue;
            ProductVariantEntity variant = resolveVariant(d);
            if (variant == null) continue;

            // Chỉ nhả nếu đang còn giữ: có RESERVE và chưa từng RELEASE / SALE_OUT.
            if (!hasReservedActive(d.getId())) {
                continue;
            }
            int updated = variantRepository.releaseStock(variant.getId(), qty);
            if (updated == 0) {
                log.warn("releaseForOrder: reserved < qty cho variant={} orderDetail={} qty={} — bỏ qua",
                        variant.getId(), d.getId(), qty);
                continue;
            }
            writeLedger(variant, d, InventoryMovementType.RELEASE, -qty, "Nhả hàng khi hủy đơn / bỏ thanh toán");
        }
    }

    @Override
    @Transactional
    public void commitSaleForOrder(List<OrderDetailEntity> details) {
        if (details == null) return;
        for (OrderDetailEntity d : details) {
            int qty = qtyOf(d);
            if (qty <= 0) continue;
            ProductVariantEntity variant = resolveVariant(d);
            if (variant == null) continue;

            // Idempotency: đã xuất kho cho dòng này rồi thì bỏ qua.
            if (ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.SALE_OUT)) {
                continue;
            }
            // Nếu chưa từng giữ (đơn cũ trước khi có tính năng kho) thì không trừ để tránh âm.
            if (!ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.RESERVE)) {
                log.warn("commitSaleForOrder: orderDetail={} chưa có RESERVE — bỏ qua xuất kho (đơn cũ?)", d.getId());
                continue;
            }
            int updated = variantRepository.commitSaleStock(variant.getId(), qty);
            if (updated == 0) {
                log.warn("commitSaleForOrder: không đủ onHand/reserved để xuất cho variant={} orderDetail={} qty={}",
                        variant.getId(), d.getId(), qty);
                continue;
            }
            writeLedger(variant, d, InventoryMovementType.SALE_OUT, -qty, "Xuất kho khi đơn hoàn thành");
        }
    }

    @Override
    @Transactional
    public void restockForOrder(List<OrderDetailEntity> details, boolean restockToSellable) {
        if (details == null) return;
        for (OrderDetailEntity d : details) {
            int qty = qtyOf(d);
            if (qty <= 0) continue;
            ProductVariantEntity variant = resolveVariant(d);
            if (variant == null) continue;

            // Idempotency: đã nhập lại (tốt hoặc loại) cho dòng này rồi thì bỏ qua.
            if (ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.RETURN_IN)
                    || ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.RETURN_SCRAP)) {
                continue;
            }
            // Chỉ nhập lại được những hàng đã thực sự xuất kho.
            if (!ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.SALE_OUT)) {
                log.warn("restockForOrder: orderDetail={} chưa có SALE_OUT — bỏ qua nhập lại", d.getId());
                continue;
            }

            if (restockToSellable) {
                variantRepository.addOnHand(variant.getId(), qty);
                writeLedger(variant, d, InventoryMovementType.RETURN_IN, qty,
                        "Nhập lại kho do hoàn hàng (hàng còn tốt)");
            } else {
                // Hàng lỗi: không cộng vào tồn bán được, chỉ ghi sổ kho loại.
                writeLedger(variant, d, InventoryMovementType.RETURN_SCRAP, 0,
                        "Hàng hoàn bị lỗi — đưa vào kho loại, không cộng tồn bán được (SL=" + qty + ")");
            }
        }
    }

    // ============================ QUẢN LÝ KHO ============================

    @Override
    @Transactional
    public InventoryStockResponse importStock(Long variantId, int quantity, String note) {
        if (quantity <= 0) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Số lượng nhập phải > 0");
        }
        ProductVariantEntity variant = getVariantOrThrow(variantId);
        variantRepository.addOnHand(variantId, quantity);
        writeLedger(variant, null, InventoryMovementType.IMPORT, quantity,
                note != null && !note.isBlank() ? note : "Nhập kho");
        return getStock(variantId);
    }

    @Override
    @Transactional
    public InventoryStockResponse adjustOnHand(Long variantId, int newOnHand, String note) {
        if (newOnHand < 0) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Tồn kho không được âm");
        }
        ProductVariantEntity variant = getVariantOrThrow(variantId);
        int before = variant.getOnHand() != null ? variant.getOnHand() : 0;
        int delta = newOnHand - before;
        variantRepository.setOnHand(variantId, newOnHand);
        writeLedger(variant, null, InventoryMovementType.ADJUST, delta,
                (note != null && !note.isBlank() ? note : "Điều chỉnh/kiểm kê")
                        + " (" + before + " → " + newOnHand + ")");
        return getStock(variantId);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStockResponse getStock(Long variantId) {
        return toStockResponse(getVariantOrThrow(variantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLedgerResponse> getLedger(Long variantId) {
        getVariantOrThrow(variantId);
        return ledgerRepository.findByVariant_IdOrderByIdDesc(variantId).stream()
                .map(this::toLedgerResponse)
                .toList();
    }

    // ============================ HELPERS ============================

    private boolean hasReservedActive(Long orderDetailId) {
        long reserve = ledgerRepository.countByOrderDetail_IdAndMovementType(orderDetailId, InventoryMovementType.RESERVE);
        if (reserve == 0) return false;
        long release = ledgerRepository.countByOrderDetail_IdAndMovementType(orderDetailId, InventoryMovementType.RELEASE);
        long saleOut = ledgerRepository.countByOrderDetail_IdAndMovementType(orderDetailId, InventoryMovementType.SALE_OUT);
        return reserve > (release + saleOut);
    }

    /**
     * Ghi một bút toán sổ cái. {@code quantity} là delta có dấu; {@code sumBegin}/{@code sumEnd}
     * luôn phản ánh số dư onHand trước/sau (đọc lại sau khi đã cập nhật bộ đếm).
     */
    private void writeLedger(ProductVariantEntity variant, OrderDetailEntity detail,
                             InventoryMovementType type, int quantityDelta, String note) {
        int onHandAfter = currentOnHand(variant.getId());
        boolean affectsOnHand = type == InventoryMovementType.IMPORT
                || type == InventoryMovementType.ADJUST
                || type == InventoryMovementType.SALE_OUT
                || type == InventoryMovementType.RETURN_IN;
        int sumBegin = affectsOnHand ? onHandAfter - quantityDelta : onHandAfter;
        ledgerRepository.save(InventoryLedgerEntity.builder()
                .variant(variant)
                .orderDetail(detail)
                .movementType(type)
                .quantity(quantityDelta)
                .sumBegin(sumBegin)
                .sumEnd(onHandAfter)
                .note(note)
                .build());
    }

    private int currentOnHand(Long variantId) {
        Integer v = variantRepository.fetchOnHand(variantId);
        return v != null ? v : 0;
    }

    private int currentAvailable(Long variantId) {
        Integer v = variantRepository.fetchAvailable(variantId);
        return v != null ? v : 0;
    }

    /**
     * OrderDetail trong hệ thống luôn có productVariant; vẫn fallback default variant cho an toàn.
     * Fetch kèm product để truy cập tên/sku an toàn ngay cả khi persistence context đã bị clear
     * sau các @Modifying query.
     */
    private ProductVariantEntity resolveVariant(OrderDetailEntity d) {
        if (d.getProductVariant() != null && d.getProductVariant().getId() != null) {
            return variantRepository.findWithProductAndPricesById(d.getProductVariant().getId()).orElse(null);
        }
        if (d.getProduct() != null && d.getProduct().getId() != null) {
            ProductVariantEntity dv = variantRepository
                    .findFirstByProduct_IdAndActiveTrueOrderBySortOrderAscIdAsc(d.getProduct().getId())
                    .orElse(null);
            return dv == null ? null : variantRepository.findWithProductAndPricesById(dv.getId()).orElse(dv);
        }
        log.warn("resolveVariant: orderDetail={} không xác định được biến thể — bỏ qua đồng bộ kho", d.getId());
        return null;
    }

    private ProductVariantEntity getVariantOrThrow(Long variantId) {
        return variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy biến thể (variant) id=" + variantId));
    }

    private static int qtyOf(OrderDetailEntity d) {
        return d.getQuantity() != null ? d.getQuantity() : 0;
    }

    private static String skuOf(ProductVariantEntity v) {
        return v.getSkuCode() != null ? v.getSkuCode() : String.valueOf(v.getId());
    }

    private static String productName(ProductVariantEntity v) {
        ProductEntity p = v.getProduct();
        return p != null && p.getProductName() != null ? p.getProductName() : "#" + v.getId();
    }

    private InventoryStockResponse toStockResponse(ProductVariantEntity v) {
        ProductEntity p = v.getProduct();
        // Đọc tồn từ DB (scalar) để phản ánh đúng sau các bulk UPDATE trong cùng transaction.
        int onHand = currentOnHand(v.getId());
        Integer reservedV = variantRepository.fetchReserved(v.getId());
        int reserved = reservedV != null ? reservedV : 0;
        return InventoryStockResponse.builder()
                .variantId(v.getId())
                .skuCode(v.getSkuCode())
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getProductName() : null)
                .onHand(onHand)
                .reserved(reserved)
                .available(onHand - reserved)
                .build();
    }

    private InventoryLedgerResponse toLedgerResponse(InventoryLedgerEntity e) {
        return InventoryLedgerResponse.builder()
                .id(e.getId())
                .variantId(e.getVariant() != null ? e.getVariant().getId() : null)
                .movementType(e.getMovementType() != null ? e.getMovementType().name() : null)
                .quantity(e.getQuantity())
                .sumBegin(e.getSumBegin())
                .sumEnd(e.getSumEnd())
                .orderDetailId(e.getOrderDetail() != null ? e.getOrderDetail().getId() : null)
                .note(e.getNote())
                .createdDate(e.getCreatedDate())
                .build();
    }
}
