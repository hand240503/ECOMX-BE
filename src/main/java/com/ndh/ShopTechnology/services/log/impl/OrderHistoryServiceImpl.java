package com.ndh.ShopTechnology.services.log.impl;

import com.ndh.ShopTechnology.entities.log.OrderHistoryEntity;
import com.ndh.ShopTechnology.entities.order.OrderEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.OrderHistoryRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.log.OrderHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderHistoryServiceImpl implements OrderHistoryService {

    private final OrderHistoryRepository orderHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderStatusChange(OrderEntity order,
                                     Integer oldStatus,
                                     Integer newStatus,
                                     String note) {
        try {
            String username  = resolveCurrentUsername();
            UserEntity actor = resolveCurrentUser(username);

            OrderHistoryEntity entry = OrderHistoryEntity.builder()
                    .order(order)
                    .changeType(OrderHistoryEntity.CHANGE_TYPE_ORDER_STATUS)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .note(note)
                    .changedByUser(actor)
                    .changedByUsername(username)
                    .createdAt(new Date())
                    .build();

            orderHistoryRepository.save(entry);
        } catch (Exception e) {
            log.error("[OrderHistory] Failed to log order status change. orderId={} {} -> {}",
                    order.getId(), oldStatus, newStatus, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReturnRefundStatusChange(OrderEntity order,
                                            Integer oldReturnRefundStatus,
                                            Integer newReturnRefundStatus,
                                            String note) {
        try {
            String username  = resolveCurrentUsername();
            UserEntity actor = resolveCurrentUser(username);

            OrderHistoryEntity entry = OrderHistoryEntity.builder()
                    .order(order)
                    .changeType(OrderHistoryEntity.CHANGE_TYPE_RETURN_REFUND_STATUS)
                    .oldReturnRefundStatus(oldReturnRefundStatus)
                    .newReturnRefundStatus(newReturnRefundStatus)
                    .note(note)
                    .changedByUser(actor)
                    .changedByUsername(username)
                    .createdAt(new Date())
                    .build();

            orderHistoryRepository.save(entry);
        } catch (Exception e) {
            log.error("[OrderHistory] Failed to log return/refund status change. orderId={} {} -> {}",
                    order.getId(), oldReturnRefundStatus, newReturnRefundStatus, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryEntity> getHistory(Long orderId) {
        return orderHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryEntity> getOrderStatusHistory(Long orderId) {
        return orderHistoryRepository.findByOrderIdAndChangeTypeOrderByCreatedAtDesc(
                orderId, OrderHistoryEntity.CHANGE_TYPE_ORDER_STATUS);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryEntity> getReturnRefundHistory(Long orderId) {
        return orderHistoryRepository.findByOrderIdAndChangeTypeOrderByCreatedAtDesc(
                orderId, OrderHistoryEntity.CHANGE_TYPE_RETURN_REFUND_STATUS);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "system";
        return auth.getName();
    }

    private UserEntity resolveCurrentUser(String username) {
        if (username == null || "system".equals(username) || "anonymousUser".equals(username)) {
            return null;
        }
        return userRepository.findOneByUsername(username).orElse(null);
    }
}
