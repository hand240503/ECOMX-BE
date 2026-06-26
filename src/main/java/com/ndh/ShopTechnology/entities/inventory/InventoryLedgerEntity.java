package com.ndh.ShopTechnology.entities.inventory;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import com.ndh.ShopTechnology.entities.store.StoreEntity;
import com.ndh.ShopTechnology.enums.inventory.InventoryMovementType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Sổ cái tồn kho — ghi từng lần biến động kho theo biến thể (SKU).
 *
 * <p>Mỗi dòng lưu: loại biến động, số lượng delta, và số dư onHand trước/sau
 * ({@code sumBegin} / {@code sumEnd}) để truy vết và đối soát. Các bút toán gắn
 * với một {@link OrderDetailEntity} khi phát sinh từ đơn hàng; nhập kho / điều
 * chỉnh thủ công thì {@code orderDetail} để null.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "InventoryLedger")
@Table(
        name = "inventory_ledger",
        indexes = {
                @Index(name = "idx_inv_ledger_variant", columnList = "variant_id"),
                @Index(name = "idx_inv_ledger_store", columnList = "store_id"),
                @Index(name = "idx_inv_ledger_order_detail", columnList = "order_detail_id"),
                @Index(name = "idx_inv_ledger_type", columnList = "movement_type")
        })
public class InventoryLedgerEntity extends BaseEntity {

    public static final String COL_VARIANT_ID       = "variant_id";
    public static final String COL_STORE_ID         = "store_id";
    public static final String COL_ORDER_DETAIL_ID  = "order_detail_id";
    public static final String COL_MOVEMENT_TYPE    = "movement_type";
    public static final String COL_QUANTITY         = "quantity";
    public static final String COL_SUM_BEGIN        = "sum_begin";
    public static final String COL_SUM_END          = "sum_end";
    public static final String COL_NOTE             = "note";

    /** Biến thể (SKU) bị tác động. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COL_VARIANT_ID, nullable = false)
    private ProductVariantEntity variant;

    /** Kho phát sinh biến động (null với bút toán cũ trước khi có tính năng đa kho). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_STORE_ID, nullable = true)
    private StoreEntity store;

    /** Dòng đơn hàng nguồn (null nếu là nhập kho / điều chỉnh thủ công). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_ORDER_DETAIL_ID, nullable = true)
    private OrderDetailEntity orderDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = COL_MOVEMENT_TYPE, nullable = false, length = 32)
    private InventoryMovementType movementType;

    /** Delta (có dấu) áp dụng cho bộ đếm tương ứng với movementType. */
    @Column(name = COL_QUANTITY, nullable = false)
    private Integer quantity;

    /** Số dư onHand trước biến động. */
    @Column(name = COL_SUM_BEGIN)
    private Integer sumBegin;

    /** Số dư onHand sau biến động. */
    @Column(name = COL_SUM_END)
    private Integer sumEnd;

    @Column(name = COL_NOTE, columnDefinition = "TEXT")
    private String note;
}
