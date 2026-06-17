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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** sessionId -> thời điểm (epoch ms) gọi querydr gần nhất, để giãn tần suất tránh lỗi 94. */
    private final Map<Long, Long> lastQueryAt = new ConcurrentHashMap<>();

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

        long now = System.currentTimeMillis();
        long windowMs = Math.max(1, props.getReconcileWindowSeconds()) * 1000L;
        long minIntervalMs = Math.max(0, props.getReconcileMinIntervalMs());
        Date createdAfter = new Date(now - windowMs);

        List<CheckoutSessionEntity> pending = checkoutSessionRepository
                .findByStatusAndCreatedDateAfter(CheckoutSessionStatus.PENDING, createdAfter);

        // Dọn các entry quá cũ (ngoài cửa sổ) để map không phình to.
        long cutoff = now - windowMs - minIntervalMs;
        for (Iterator<Map.Entry<Long, Long>> it = lastQueryAt.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue() < cutoff) {
                it.remove();
            }
        }

        if (pending.isEmpty()) {
            return;
        }

        for (CheckoutSessionEntity session : pending) {
            long id = session.getId();
            Long last = lastQueryAt.get(id);
            // Giãn tần suất: bỏ qua nếu vừa query gần đây (tránh VNPAY trả 94 duplicate).
            if (last != null && (now - last) < minIntervalMs) {
                continue;
            }
            lastQueryAt.put(id, now);
            try {
                vnpayService.reconcilePendingCheckoutSession(id);
            } catch (Exception e) {
                log.warn("VNPAY reconcile failed for sessionId={}: {}", id, e.toString());
            }
        }
    }
}
