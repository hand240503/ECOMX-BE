package com.ndh.ShopTechnology.services.inventory;

import com.ndh.ShopTechnology.dto.response.inventory.InventoryLedgerResponse;
import com.ndh.ShopTechnology.dto.response.inventory.InventoryStockResponse;
import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;

import java.util.List;

/**
 * Đồng bộ tồn kho cho mọi luồng liên quan đến kho hàng.
 *
 * <p>Tất cả các method theo đơn hàng đều <b>idempotent</b> (an toàn khi gọi lặp
 * do retry / event trùng / double-click) và phải chạy trong cùng transaction với
 * thao tác đơn hàng để đảm bảo nhất quán.
 */
public interface InventoryService {

    /**
     * Giữ hàng khi đặt đơn.
     *
     * @param throwIfInsufficient true (đơn chưa thanh toán, COD): không đủ tồn → ném CONFLICT,
     *                            rollback việc tạo đơn (chống bán âm);
     *                            false (đơn đã thanh toán, VNPAY): vẫn giữ chỗ best-effort,
     *                            cho phép oversell và chỉ ghi cảnh báo (không từ chối đơn đã trả tiền).
     */
    void reserveForOrder(List<OrderDetailEntity> details, boolean throwIfInsufficient);

    /** Nhả hàng đã giữ khi hủy đơn / bỏ thanh toán (chỉ nếu đang còn giữ). */
    void releaseForOrder(List<OrderDetailEntity> details);

    /** Xuất kho khi đơn hoàn thành: trừ onHand và reserved. */
    void commitSaleForOrder(List<OrderDetailEntity> details);

    /**
     * Nhập lại kho khi hoàn hàng đã hoàn tiền (REFUNDED).
     *
     * @param restockToSellable true = hàng còn tốt, cộng lại tồn bán được;
     *                          false = hàng lỗi, chỉ ghi sổ kho loại (không cộng tồn).
     */
    void restockForOrder(List<OrderDetailEntity> details, boolean restockToSellable);

    /** Nhập kho thủ công (phiếu nhập). */
    InventoryStockResponse importStock(Long variantId, int quantity, String note);

    /** Điều chỉnh / kiểm kê: đặt thẳng onHand về giá trị mới. */
    InventoryStockResponse adjustOnHand(Long variantId, int newOnHand, String note);

    /** Xem tồn hiện tại của một biến thể. */
    InventoryStockResponse getStock(Long variantId);

    /** Lịch sử biến động kho của một biến thể. */
    List<InventoryLedgerResponse> getLedger(Long variantId);

    /** Danh sách tồn kho tất cả biến thể (lọc theo tên sản phẩm / SKU; q rỗng = tất cả). */
    List<InventoryStockResponse> listStocks(String q);
}
