package com.ndh.ShopTechnology.services.payment;

import com.ndh.ShopTechnology.config.VnpayProperties;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionEntity;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionStatus;
import com.ndh.ShopTechnology.repository.CheckoutSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * Định kỳ gọi querydr để đối soát các phiên VNPAY còn PENDING — chỉ trong cửa sổ
 * {@code vnpay.reconcile-window-seconds} (mặc định 2 phút) tính từ lúc tạo phiên.
 * Hết cửa sổ, scheduler không truy vấn phiên đó nữa (không treo vô hạn).
 */
@Component
public class VnpayReconcileScheduler {

    private static final Logger log = LoggerFactory.getLogger(VnpayReconcileScheduler.class);

    private final VnpayService vnpayService;
    private final VnpayProperties props;
    private final CheckoutSessionRepository checkoutSessionRepository;

    public VnpayReconcileScheduler(
            VnpayService vnpayService,
            VnpayProperties props,
            CheckoutSessionRepository checkoutSessionRepository) {
        this.vnpayService = vnpayService;
        this.props = props;
        this.checkoutSessionRepository = checkoutSessionRepository;
    }

    @Scheduled(fixedDelayString = "${vnpay.reconcile-poll-ms:15000}", initialDelay = 20000)
    public void reconcilePendingTransactions() {
        if (!props.isEnabled()
                || !StringUtils.hasText(props.getHashSecret())
                || !StringUtils.hasText(props.getApiUrl())
                || !StringUtils.hasText(props.getTmnCode())) {
            return;
        }

        long windowMs = Math.max(1, props.getReconcileWindowSeconds()) * 1000L;
        Date createdAfter = new Date(System.currentTimeMillis() - windowMs);

        List<CheckoutSessionEntity> pending = checkoutSessionRepository
                .findByStatusAndCreatedDateAfter(CheckoutSessionStatus.PENDING, createdAfter);
        if (pending.isEmpty()) {
            return;
        }

        for (CheckoutSessionEntity session : pending) {
            try {
                vnpayService.reconcilePendingCheckoutSession(session.getId());
            } catch (Exception e) {
                log.warn("VNPAY reconcile failed for sessionId={}: {}", session.getId(), e.toString());
            }
        }
    }
}
