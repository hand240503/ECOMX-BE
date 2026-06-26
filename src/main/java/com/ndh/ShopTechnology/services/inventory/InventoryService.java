package com.ndh.ShopTechnology.services.inventory;

import com.ndh.ShopTechnology.dto.request.store.StockTransferRequest;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryLedgerResponse;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryStockResponse;
import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import com.ndh.ShopTechnology.entities.store.StoreEntity;

import java.util.List;

/**
 * Đồng bộ tồn kho ĐA KHO cho mọi luồng liên quan đến kho hàng.
 *
 * <p>Tồn được quản lý ở cấp (kho × biến thể) trong bảng store_stock. Tồn aggregate
 * trên product_variant được đồng bộ tự động = tổng tồn các kho (dùng cho hiển thị).
 *
 * <p>Tất cả các method theo đơn hàng đều <b>idempotent</b> và phải chạy trong cùng
 * transaction với thao tác đơn hàng để đảm bảo nhất quán.
 */
public interface InventoryService {

    /**
     * Giữ hàng tại {@code store} khi đặt đơn.
     *
     * @param throwIfInsufficient true (COD): không đủ tồn tại kho → ném CONFLICT, rollback đơn;
     *                            false (đã thanh toán): giữ chỗ best-effort, cho phép oversell.
     */
    void reserveForOrder(StoreEntity store, List<OrderDetailEntity> details, boolean throwIfInsufficient);

    /** Nhả hàng đã giữ tại kho khi hủy đơn / bỏ thanh toán. */
    void releaseForOrder(StoreEntity store, List<OrderDetailEntity> details);

    /** Xuất kho tại kho khi đơn hoàn thành. */
    void commitSaleForOrder(StoreEntity store, List<OrderDetailEntity> details);

    /** Nhập lại kho tại kho khi hoàn hàng đã hoàn tiền. */
    void restockForOrder(StoreEntity store, List<OrderDetailEntity> details, boolean restockToSellable);

    // ============================ QUẢN LÝ KHO ============================

    /** Nhập kho thủ công vào một kho cụ thể. */
    InventoryStockResponse importStock(Long storeId, Long variantId, int quantity, String note);

    /** Điều chỉnh / kiểm kê: đặt thẳng onHand của biến thể tại một kho. */
    InventoryStockResponse adjustOnHand(Long storeId, Long variantId, int newOnHand, String note);

    /** Chuyển hàng từ kho này sang kho khác (xuất kho nguồn, nhập kho đích). */
    void transfer(StockTransferRequest request);

    /** Xem tồn hiện tại của một biến thể tại một kho. */
    InventoryStockResponse getStock(Long storeId, Long variantId);

    /** Danh sách tồn kho của một kho (lọc theo tên SP / SKU). */
    List<InventoryStockResponse> listStocks(Long storeId, String q);

    /** Tồn của một biến thể tại tất cả các kho. */
    List<InventoryStockResponse> listStocksByVariant(Long variantId);

    /** Lịch sử biến động kho của một biến thể tại một kho. */
    List<InventoryLedgerResponse> getLedger(Long storeId, Long variantId);
}
