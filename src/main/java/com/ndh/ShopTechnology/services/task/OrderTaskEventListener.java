package com.ndh.ShopTechnology.services.task;

import com.ndh.ShopTechnology.enums.task.TaskSourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTaskEventListener {

    private final TaskService taskService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            String code  = event.getOrderCode() != null ? event.getOrderCode() : "#" + event.getOrderId();
            String title = "Chuan bi don hang " + code;
            String desc  = "Don hang " + code + " - " + event.getTotalItems() + " san pham"
                         + (event.getTotalQuantity() > 0 ? " (tong " + event.getTotalQuantity() + " cai)" : "");
            taskService.autoCreateFromSource(
                TaskSourceType.ORDER,
                event.getOrderId(),
                null,
                title,
                desc
            );
            log.info("Auto-task created for order: {} ({})", event.getOrderId(), code);
        } catch (Exception ex) {
            log.error("Failed to auto-create task for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderReturned(OrderReturnedEvent event) {
        try {
            taskService.autoCreateFromSource(
                TaskSourceType.RETURN,
                event.getOrderId(),
                null,
                "Xu ly hoan tra don hang #" + event.getOrderId(),
                null
            );
            log.info("Auto-task created for return: orderId={}", event.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to auto-create return task for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        try {
            taskService.cancelTaskBySource(TaskSourceType.ORDER, event.getOrderId());
            log.info("Auto-cancelled ORDER task for order: {}", event.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to auto-cancel task for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
