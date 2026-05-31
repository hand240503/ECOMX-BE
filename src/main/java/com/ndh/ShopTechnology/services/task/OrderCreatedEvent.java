package com.ndh.ShopTechnology.services.task;

import org.springframework.context.ApplicationEvent;

/**
 * Published after an order is successfully persisted.
 * Carries enough order data for the task listener to build a meaningful task
 * without needing to re-query the database.
 */
public class OrderCreatedEvent extends ApplicationEvent {

    private final Long   orderId;
    private final String orderCode;
    private final int    totalItems;     // so san pham (line items)
    private final int    totalQuantity;  // tong so luong

    public OrderCreatedEvent(Object source, Long orderId, String orderCode,
                             int totalItems, int totalQuantity) {
        super(source);
        this.orderId       = orderId;
        this.orderCode     = orderCode;
        this.totalItems    = totalItems;
        this.totalQuantity = totalQuantity;
    }

    /** Backward-compat constructor (VNPAY path where details may not be loaded) */
    public OrderCreatedEvent(Object source, Long orderId) {
        this(source, orderId, "#" + orderId, 0, 0);
    }

    public Long   getOrderId()       { return orderId;       }
    public String getOrderCode()     { return orderCode;     }
    public int    getTotalItems()    { return totalItems;    }
    public int    getTotalQuantity() { return totalQuantity; }
}
