package com.ndh.ShopTechnology.services.task;

import org.springframework.context.ApplicationEvent;

public class OrderCancelledEvent extends ApplicationEvent {
    private final Long orderId;

    public OrderCancelledEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }

    public Long getOrderId() { return orderId; }
}
