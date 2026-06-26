package com.ndh.ShopTechnology.enums.inventory;

/**
 * Loại biến động tồn kho ghi trong sổ cái (inventory_ledger).
 *
 * <p>Quy ước dấu của {@code quantity} (delta áp dụng cho bộ đếm tương ứng):
 * <ul>
 *   <li>{@link #IMPORT}   : +onHand   — nhập kho.</li>
 *   <li>{@link #ADJUST}   : ±onHand   — điều chỉnh/kiểm kê thủ công.</li>
 *   <li>{@link #RESERVE}  : +reserved — giữ hàng khi đặt đơn.</li>
 *   <li>{@link #RELEASE}  : -reserved — nhả hàng khi hủy đơn / bỏ thanh toán.</li>
 *   <li>{@link #SALE_OUT} : -onHand và -reserved — xuất kho khi đơn hoàn thành.</li>
 *   <li>{@link #RETURN_IN}: +onHand   — nhập lại kho khi hoàn hàng (hàng còn tốt).</li>
 *   <li>{@link #RETURN_SCRAP}: 0 onHand bán được — hàng hoàn bị lỗi, đưa vào kho loại.</li>
 *   <li>{@link #TRANSFER_OUT}: -onHand — xuất chuyển sang kho khác.</li>
 *   <li>{@link #TRANSFER_IN} : +onHand — nhập do chuyển từ kho khác sang.</li>
 * </ul>
 */
public enum InventoryMovementType {
    IMPORT,
    ADJUST,
    RESERVE,
    RELEASE,
    SALE_OUT,
    RETURN_IN,
    RETURN_SCRAP,
    TRANSFER_OUT,
    TRANSFER_IN
}
