package com.ndh.ShopTechnology.services.log.impl;

import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.AdminActivityLogRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.log.AdminActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminActivityLogServiceImpl implements AdminActivityLogService {

    private final AdminActivityLogRepository adminActivityLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdminActivityLogEntity log(String action,
                                      String entityType,
                                      Long entityId,
                                      String entityLabel,
                                      String snapshotBefore,
                                      String snapshotAfter) {
        try {
            String username  = resolveCurrentUsername();
            UserEntity actor = resolveCurrentUser(username);
            String ip        = resolveClientIp();
            String userAgent = resolveUserAgent();

            AdminActivityLogEntity entry = AdminActivityLogEntity.builder()
                    .actorUser(actor)
                    .actorUsername(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityLabel(entityLabel)
                    .snapshotBefore(snapshotBefore)
                    .snapshotAfter(snapshotAfter)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .createdAt(new Date())
                    .build();

            return adminActivityLogRepository.save(entry);
        } catch (Exception e) {
            // Log nhưng không throw – không để lỗi audit chặn nghiệp vụ chính
            log.error("[AdminActivityLog] Failed to save log entry. action={} entityType={} entityId={}",
                    action, entityType, entityId, e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminActivityLogEntity> getByEntity(String entityType, Long entityId) {
        return adminActivityLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminActivityLogEntity> search(Long actorUserId,
                                               String entityType,
                                               String action,
                                               Date from,
                                               Date to,
                                               Pageable pageable) {
        // actorRoleCode = null → không lọc theo role (dùng /admin/history?actorRoleCode= cho filter role)
        return adminActivityLogRepository.search(actorUserId, entityType, action, from, to, null, pageable);
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

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // X-Forwarded-For có thể chứa nhiều IP, lấy cái đầu tiên
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUserAgent() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            String ua = attrs.getRequest().getHeader("User-Agent");
            return ua != null && ua.length() > 512 ? ua.substring(0, 512) : ua;
        } catch (Exception e) {
            return null;
        }
    }
}
