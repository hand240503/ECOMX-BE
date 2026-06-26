package com.ndh.ShopTechnology.services.inventory.impl;

import com.ndh.ShopTechnology.dto.request.store.StockTransferRequest;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryLedgerResponse;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryStockResponse;
import com.ndh.ShopTechnology.entities.inventory.InventoryLedgerEntity;
import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.store.StoreEntity;
import com.ndh.ShopTechnology.entities.store.StoreStockEntity;
import com.ndh.ShopTechnology.enums.inventory.InventoryMovementType;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.InventoryLedgerRepository;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.repository.StoreRepository;
import com.ndh.ShopTechnology.repository.StoreStockRepository;
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
    private final StoreRepository storeRepository;
    private final StoreStockRepository storeStockRepository;

    public InventoryServiceImpl(ProductVariantRepository variantRepository,
                                InventoryLedgerRepository ledgerRepository,
                                StoreRepository storeRepository,
                                StoreStockRepository storeStockRepository) {
        this.variantRepository = variantRepository;
        this.ledgerRepository = ledgerRepository;
        this.storeRepository = storeRepository;
        this.storeStockRepository = storeStockRepository;
    }

    // ============================ LUỒNG ĐƠN HÀNG ============================

    @Override
    @Transactional
    public void reserveForOrder(StoreEntity store, List<OrderDetailEntity> details, boolean throwIfInsufficient) {
        if (details == null) return;
        if (store == null || store.getId() == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Đơn hàng chưa xác định kho (store) để trừ tồn.");
        }
        Long storeId = store.getId();
        for (OrderDetailEntity d : details) {
            int qty = qtyOf(d);
            if (qty <= 0) continue;
            ProductVariantEntity variant = resolveVariant(d);
            if (variant == null) continue;

            // Idempotency: đã giữ cho dòng đơn này rồi thì bỏ qua.
            if (ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.RESERVE)) {
                continue;
            }

            int updated = storeStockRepository.reserveStock(storeId, variant.getId(), qty);
            if (updated == 0) {
                if (throwIfInsufficient) {
                    int available = currentAvailable(storeId, variant.getId());
                    throw new CustomApiException(HttpStatus.CONFLICT,
                            "Không đủ hàng tại kho '" + store.getName() + "' cho sản phẩm '" + productName(variant)
                                    + "' (SKU " + skuOf(variant) + "). Cần " + qty
                                    + ", còn lại " + available + ".");
                }
                // Đơn đã thanh toán: vẫn giữ chỗ (cho phép oversell), ghi cảnh báo để admin xử lý.
                getOrCreateStoreStock(store, variant);
                storeStockRepository.forceReserve(storeId, variant.getId(), qty);
                log.warn("reserveForOrder: OVERSELL — giữ chỗ vượt tồn tại kho={} cho đơn đã thanh toán. variant={} orderDetail={} qty={}",
                        storeId, variant.getId(), d.getId(), qty);
            }
            syncAggregate(variant.getId());
            writeLedger(store, variant, d, InventoryMovementType.RESERVE, qty, "Giữ hàng khi đặt đơn");
        }
    }

    @Override
    @Transactional
    public void releaseForOrder(StoreEntity store, List<OrderDetailEntity> details) {
        if (details == null || store == null || store.getId() == null) return;
        Long storeId = store.getId();
        for (OrderDetailEntity d : details) {
            int qty = qtyOf(d);
            if (qty <= 0) continue;
            ProductVariantEntity variant = resolveVariant(d);
            if (variant == null) continue;

            if (!hasReservedActive(d.getId())) continue;
            int updated = storeStockRepository.releaseStock(storeId, variant.getId(), qty);
            if (updated == 0) {
                log.warn("releaseForOrder: reserved < qty tại kho={} variant={} orderDetail={} qty={} — bỏ qua",
                        storeId, variant.getId(), d.getId(), qty);
                continue;
            }
            syncAggregate(variant.getId());
            writeLedger(store, variant, d, InventoryMovementType.RELEASE, -qty,
                    "Nhả hàng khi hủy đơn / bỏ thanh toán");
        }
    }

    @Override
    @Transactional
    public void commitSaleForOrder(StoreEntity store, List<OrderDetailEntity> details) {
        if (details == null || store == null || store.getId() == null) return;
        Long storeId = store.getId();
        for (OrderDetailEntity d : details) {
            int qty = qtyOf(d);
            if (qty <= 0) continue;
            ProductVariantEntity variant = resolveVariant(d);
            if (variant == null) continue;

            if (ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.SALE_OUT)) {
                continue;
            }
            if (!ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.RESERVE)) {
                log.warn("commitSaleForOrder: orderDetail={} chưa có RESERVE — bỏ qua xuất kho (đơn cũ?)", d.getId());
                continue;
            }
            int updated = storeStockRepository.commitSaleStock(storeId, variant.getId(), qty);
            if (updated == 0) {
                log.warn("commitSaleForOrder: không đủ onHand/reserved tại kho={} variant={} orderDetail={} qty={}",
                        storeId, variant.getId(), d.getId(), qty);
                continue;
            }
            syncAggregate(variant.getId());
            writeLedger(store, variant, d, InventoryMovementType.SALE_OUT, -qty, "Xuất kho khi đơn hoàn thành");
        }
    }

    @Override
    @Transactional
    public void restockForOrder(StoreEntity store, List<OrderDetailEntity> details, boolean restockToSellable) {
        if (details == null || store == null || store.getId() == null) return;
        Long storeId = store.getId();
        for (OrderDetailEntity d : details) {
            int qty = qtyOf(d);
            if (qty <= 0) continue;
            ProductVariantEntity variant = resolveVariant(d);
            if (variant == null) continue;

            if (ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.RETURN_IN)
                    || ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.RETURN_SCRAP)) {
                continue;
            }
            if (!ledgerRepository.existsByOrderDetail_IdAndMovementType(d.getId(), InventoryMovementType.SALE_OUT)) {
                log.warn("restockForOrder: orderDetail={} chưa có SALE_OUT — bỏ qua nhập lại", d.getId());
                continue;
            }

            if (restockToSellable) {
                getOrCreateStoreStock(store, variant);
                storeStockRepository.addOnHand(storeId, variant.getId(), qty);
                syncAggregate(variant.getId());
                writeLedger(store, variant, d, InventoryMovementType.RETURN_IN, qty, "Nhập lại kho do hoàn hàng");
            } else {
                writeLedger(store, variant, d, InventoryMovementType.RETURN_SCRAP, 0,
                        "Hàng hoàn bị lỗi — đưa vào kho loại, không cộng tồn bán được (SL=" + qty + ")");
            }
        }
    }

    // ============================ QUẢN LÝ KHO ============================

    @Override
    @Transactional
    public InventoryStockResponse importStock(Long storeId, Long variantId, int quantity, String note) {
        if (quantity <= 0) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Số lượng nhập phải > 0");
        }
        StoreEntity store = getStoreOrThrow(storeId);
        ProductVariantEntity variant = getVariantOrThrow(variantId);
        getOrCreateStoreStock(store, variant);
        storeStockRepository.addOnHand(storeId, variantId, quantity);
        syncAggregate(variantId);
        writeLedger(store, variant, null, InventoryMovementType.IMPORT, quantity,
                note != null && !note.isBlank() ? note : "Nhập kho");
        return getStock(storeId, variantId);
    }

    @Override
    @Transactional
    public InventoryStockResponse adjustOnHand(Long storeId, Long variantId, int newOnHand, String note) {
        if (newOnHand < 0) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Tồn kho không được âm");
        }
        StoreEntity store = getStoreOrThrow(storeId);
        ProductVariantEntity variant = getVariantOrThrow(variantId);
        getOrCreateStoreStock(store, variant);
        int before = currentOnHand(storeId, variantId);
        int delta = newOnHand - before;
        storeStockRepository.setOnHand(storeId, variantId, newOnHand);
        syncAggregate(variantId);
        writeLedger(store, variant, null, InventoryMovementType.ADJUST, delta,
                (note != null && !note.isBlank() ? note : "Điều chỉnh/kiểm kê")
                        + " (" + before + " → " + newOnHand + ")");
        return getStock(storeId, variantId);
    }

    @Override
    @Transactional
    public void transfer(StockTransferRequest request) {
        Long fromId = request.getFromStoreId();
        Long toId = request.getToStoreId();
        if (fromId == null || toId == null || fromId.equals(toId)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Kho nguồn và kho đích phải khác nhau.");
        }
        StoreEntity from = getStoreOrThrow(fromId);
        StoreEntity to = getStoreOrThrow(toId);
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Danh sách sản phẩm chuyển không được rỗng.");
        }
        String baseNote = request.getNote() != null && !request.getNote().isBlank() ? request.getNote().trim() : null;
        for (StockTransferRequest.Item item : request.getItems()) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) continue;
            ProductVariantEntity variant = getVariantOrThrow(item.getVariantId());
            // Xuất kho nguồn — chỉ khi đủ tồn bán được.
            int reduced = storeStockRepository.reduceOnHandIfAvailable(fromId, variant.getId(), qty);
            if (reduced == 0) {
                int available = currentAvailable(fromId, variant.getId());
                throw new CustomApiException(HttpStatus.CONFLICT,
                        "Không đủ tồn tại kho nguồn '" + from.getName() + "' cho sản phẩm '" + productName(variant)
                                + "' (SKU " + skuOf(variant) + "). Cần " + qty + ", còn bán được " + available + ".");
            }
            syncAggregate(variant.getId());
            writeLedger(from, variant, null, InventoryMovementType.TRANSFER_OUT, -qty,
                    "Chuyển sang kho '" + to.getName() + "'" + (baseNote != null ? " — " + baseNote : ""));
            // Nhập kho đích.
            getOrCreateStoreStock(to, variant);
            storeStockRepository.addOnHand(toId, variant.getId(), qty);
            syncAggregate(variant.getId());
            writeLedger(to, variant, null, InventoryMovementType.TRANSFER_IN, qty,
                    "Nhận từ kho '" + from.getName() + "'" + (baseNote != null ? " — " + baseNote : ""));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStockResponse getStock(Long storeId, Long variantId) {
        StoreEntity store = getStoreOrThrow(storeId);
        ProductVariantEntity variant = getVariantOrThrow(variantId);
        StoreStockEntity ss = storeStockRepository.findByStore_IdAndVariant_Id(storeId, variantId).orElse(null);
        int onHand = ss != null && ss.getOnHand() != null ? ss.getOnHand() : 0;
        int reserved = ss != null && ss.getReserved() != null ? ss.getReserved() : 0;
        return toStockResponse(store, variant, onHand, reserved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStockResponse> listStocks(Long storeId, String q) {
        StoreEntity store = getStoreOrThrow(storeId);
        String key = (q != null && !q.isBlank()) ? q.trim() : null;
        return storeStockRepository.listByStore(storeId, key).stream()
                .map(ss -> toStockResponse(store, ss.getVariant(),
                        nz(ss.getOnHand()), nz(ss.getReserved())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStockResponse> listStocksByVariant(Long variantId) {
        getVariantOrThrow(variantId);
        return storeStockRepository.listByVariant(variantId).stream()
                .map(ss -> toStockResponse(ss.getStore(), ss.getVariant(),
                        nz(ss.getOnHand()), nz(ss.getReserved())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLedgerResponse> getLedger(Long storeId, Long variantId) {
        getStoreOrThrow(storeId);
        getVariantOrThrow(variantId);
        return ledgerRepository.findByStore_IdAndVariant_IdOrderByIdDesc(storeId, variantId).stream()
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

    /** Lấy hoặc tạo dòng tồn (zero) cho (kho, biến thể). */
    private StoreStockEntity getOrCreateStoreStock(StoreEntity store, ProductVariantEntity variant) {
        return storeStockRepository.findByStore_IdAndVariant_Id(store.getId(), variant.getId())
                .orElseGet(() -> storeStockRepository.save(StoreStockEntity.builder()
                        .store(store)
                        .variant(variant)
                        .onHand(0)
                        .reserved(0)
                        .build()));
    }

    /** Đồng bộ tồn aggregate trên variant = tổng tồn các kho. */
    private void syncAggregate(Long variantId) {
        variantRepository.recomputeAggregateFromStores(variantId);
    }

    private void writeLedger(StoreEntity store, ProductVariantEntity variant, OrderDetailEntity detail,
                             InventoryMovementType type, int quantityDelta, String note) {
        int onHandAfter = currentOnHand(store.getId(), variant.getId());
        boolean affectsOnHand = type == InventoryMovementType.IMPORT
                || type == InventoryMovementType.ADJUST
                || type == InventoryMovementType.SALE_OUT
                || type == InventoryMovementType.RETURN_IN
                || type == InventoryMovementType.TRANSFER_IN
                || type == InventoryMovementType.TRANSFER_OUT;
        int sumBegin = affectsOnHand ? onHandAfter - quantityDelta : onHandAfter;
        ledgerRepository.save(InventoryLedgerEntity.builder()
                .variant(variant)
                .store(store)
                .orderDetail(detail)
                .movementType(type)
                .quantity(quantityDelta)
                .sumBegin(sumBegin)
                .sumEnd(onHandAfter)
                .note(note)
                .build());
    }

    private int currentOnHand(Long storeId, Long variantId) {
        Integer v = storeStockRepository.fetchOnHand(storeId, variantId);
        return v != null ? v : 0;
    }

    private int currentAvailable(Long storeId, Long variantId) {
        Integer v = storeStockRepository.fetchAvailable(storeId, variantId);
        return v != null ? v : 0;
    }

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
        return variantRepository.findWithProductAndPricesById(variantId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy biến thể (variant) id=" + variantId));
    }

    private StoreEntity getStoreOrThrow(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy kho id=" + storeId));
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
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

    private InventoryStockResponse toStockResponse(StoreEntity store, ProductVariantEntity v, int onHand, int reserved) {
        ProductEntity p = v.getProduct();
        return InventoryStockResponse.builder()
                .storeId(store != null ? store.getId() : null)
                .storeName(store != null ? store.getName() : null)
                .variantId(v.getId())
                .skuCode(v.getSkuCode())
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getProductName() : null)
                .optionValues(v.getOptionValues())
                .onHand(onHand)
                .reserved(reserved)
                .available(onHand - reserved)
                .build();
    }

    private InventoryLedgerResponse toLedgerResponse(InventoryLedgerEntity e) {
        return InventoryLedgerResponse.builder()
                .id(e.getId())
                .variantId(e.getVariant() != null ? e.getVariant().getId() : null)
                .storeId(e.getStore() != null ? e.getStore().getId() : null)
                .storeName(e.getStore() != null ? e.getStore().getName() : null)
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
