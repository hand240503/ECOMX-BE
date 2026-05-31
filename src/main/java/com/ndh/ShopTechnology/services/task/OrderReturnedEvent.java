package com.ndh.ShopTechnology.services.task;

import org.springframework.context.ApplicationEvent;

public class OrderReturnedEvent extends ApplicationEvent {
    private final Long orderId;

    public OrderReturnedEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }

    public Long getOrderId() { return orderId; }
}
