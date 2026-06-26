package com.ndh.ShopTechnology.services.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.CbRecommendationDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.ImplicitRatingDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.InsightsPage;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.InsightsSummaryDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.SimilarityPairDto;
import com.ndh.ShopTechnology.dto.response.recommendation.insights.UserProfileDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc các bảng kết quả của pipeline recommend (item_similarity, user_ratings implicit,
 * user_preference_profile, cb_content_recommendation) và join thêm tên sản phẩm / user
 * để hiển thị trong trang admin "Phân tích gợi ý". Chỉ ĐỌC (read-only), dùng native query.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationInsightsService {

    private static final String ALGO_CF = "cf_cosine";
    private static final String ALGO_CONTENT = "content_tfidf";

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper objectMapper;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static long asLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static Long asLongOrNull(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static Integer asInt(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }

    private static Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd.doubleValue();
        return ((Number) o).doubleValue();
    }

    private static String asStr(Object o) {
        return o == null ? null : o.toString();
    }

    private int safeSize(int size) {
        return Math.min(Math.max(size, 1), 500);
    }

    // ── 1) Tổng quan ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InsightsSummaryDto getSummary() {
        InsightsSummaryDto.InsightsSummaryDtoBuilder b = InsightsSummaryDto.builder();

        // item_similarity theo thuật toán
        List<?> simRows = em.createNativeQuery(
                "SELECT algorithm, COUNT(*) FROM item_similarity GROUP BY algorithm")
                .getResultList();
        for (Object row : simRows) {
            Object[] r = (Object[]) row;
            String algo = asStr(r[0]);
            long cnt = asLong(r[1]);
            if (ALGO_CF.equals(algo)) b.cfCosinePairs(cnt);
            else if (ALGO_CONTENT.equals(algo)) b.contentTfidfPairs(cnt);
        }

        Object[] imp = (Object[]) em.createNativeQuery(
                "SELECT COUNT(*), COUNT(DISTINCT user_id), COUNT(DISTINCT product_id) "
                        + "FROM user_ratings WHERE type = 1")
                .getSingleResult();
        b.implicitRatings(asLong(imp[0]));
        b.implicitUsers(asLong(imp[1]));
        b.implicitProducts(asLong(imp[2]));

        b.userProfiles(asLong(em.createNativeQuery(
                "SELECT COUNT(*) FROM user_preference_profile").getSingleResult()));
        b.cbRecommendations(asLong(em.createNativeQuery(
                "SELECT COUNT(*) FROM cb_content_recommendation").getSingleResult()));
        b.activeProducts(asLong(em.createNativeQuery(
                "SELECT COUNT(*) FROM products WHERE status = 1").getSingleResult()));

        return b.build();
    }

    // ── 2) Độ tương đồng item-item ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InsightsPage<SimilarityPairDto> getSimilarities(
            String algorithm, Long source, int page, int size) {

        String algo = (ALGO_CONTENT.equals(algorithm)) ? ALGO_CONTENT : ALGO_CF;
        int sz = safeSize(size);
        int pg = Math.max(page, 0);

        StringBuilder where = new StringBuilder("WHERE s.algorithm = :algo ");
        if (source != null) where.append("AND s.source = :source ");

        Query countQ = em.createNativeQuery(
                "SELECT COUNT(*) FROM item_similarity s " + where);
        countQ.setParameter("algo", algo);
        if (source != null) countQ.setParameter("source", source);
        long total = asLong(countQ.getSingleResult());

        Query q = em.createNativeQuery(
                "SELECT s.source, ps.product_name, s.target, pt.product_name, "
                        + "       s.similarity, s.rank_pos "
                        + "FROM item_similarity s "
                        + "LEFT JOIN products ps ON ps.id = s.source "
                        + "LEFT JOIN products pt ON pt.id = s.target "
                        + where
                        + "ORDER BY s.source ASC, s.rank_pos ASC "
                        + "LIMIT :limit OFFSET :offset");
        q.setParameter("algo", algo);
        if (source != null) q.setParameter("source", source);
        q.setParameter("limit", sz);
        q.setParameter("offset", pg * sz);

        List<SimilarityPairDto> content = new ArrayList<>();
        for (Object row : q.getResultList()) {
            Object[] r = (Object[]) row;
            content.add(SimilarityPairDto.builder()
                    .sourceId(asLongOrNull(r[0]))
                    .sourceName(asStr(r[1]))
                    .targetId(asLongOrNull(r[2]))
                    .targetName(asStr(r[3]))
                    .similarity(r[4] == null ? null : new BigDecimal(r[4].toString()))
                    .rankPos(asInt(r[5]))
                    .algorithm(algo)
                    .build());
        }
        return InsightsPage.of(content, pg, sz, total);
    }

    // ── 3) Implicit ratings ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InsightsPage<ImplicitRatingDto> getImplicitRatings(
            Long userId, Long productId, int page, int size) {

        int sz = safeSize(size);
        int pg = Math.max(page, 0);

        StringBuilder where = new StringBuilder("WHERE r.type = 1 ");
        if (userId != null) where.append("AND r.user_id = :userId ");
        if (productId != null) where.append("AND r.product_id = :productId ");

        Query countQ = em.createNativeQuery(
                "SELECT COUNT(*) FROM user_ratings r " + where);
        if (userId != null) countQ.setParameter("userId", userId);
        if (productId != null) countQ.setParameter("productId", productId);
        long total = asLong(countQ.getSingleResult());

        Query q = em.createNativeQuery(
                "SELECT r.user_id, u.user_name, r.product_id, p.product_name, r.rating "
                        + "FROM user_ratings r "
                        + "LEFT JOIN users u ON u.id = r.user_id "
                        + "LEFT JOIN products p ON p.id = r.product_id "
                        + where
                        + "ORDER BY r.user_id ASC, r.rating DESC "
                        + "LIMIT :limit OFFSET :offset");
        if (userId != null) q.setParameter("userId", userId);
        if (productId != null) q.setParameter("productId", productId);
        q.setParameter("limit", sz);
        q.setParameter("offset", pg * sz);

        List<ImplicitRatingDto> content = new ArrayList<>();
        for (Object row : q.getResultList()) {
            Object[] r = (Object[]) row;
            content.add(ImplicitRatingDto.builder()
                    .userId(asLongOrNull(r[0]))
                    .userName(asStr(r[1]))
                    .productId(asLongOrNull(r[2]))
                    .productName(asStr(r[3]))
                    .rating(asDouble(r[4]))
                    .build());
        }
        return InsightsPage.of(content, pg, sz, total);
    }

    // ── 4) Hồ sơ sở thích user ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InsightsPage<UserProfileDto> getUserProfiles(Long userId, int page, int size) {
        int sz = safeSize(size);
        int pg = Math.max(page, 0);

        String where = userId != null ? "WHERE pf.user_id = :userId " : "";

        Query countQ = em.createNativeQuery(
                "SELECT COUNT(*) FROM user_preference_profile pf " + where);
        if (userId != null) countQ.setParameter("userId", userId);
        long total = asLong(countQ.getSingleResult());

        Query q = em.createNativeQuery(
                "SELECT pf.user_id, u.user_name, "
                        + "       CAST(pf.metadata AS CHAR), pf.updated_at "
                        + "FROM user_preference_profile pf "
                        + "LEFT JOIN users u ON u.id = pf.user_id "
                        + where
                        + "ORDER BY pf.user_id ASC "
                        + "LIMIT :limit OFFSET :offset");
        if (userId != null) q.setParameter("userId", userId);
        q.setParameter("limit", sz);
        q.setParameter("offset", pg * sz);

        List<UserProfileDto> content = new ArrayList<>();
        for (Object row : q.getResultList()) {
            Object[] r = (Object[]) row;
            content.add(UserProfileDto.builder()
                    .userId(asLongOrNull(r[0]))
                    .userName(asStr(r[1]))
                    .updatedAt(asStr(r[3]))
                    .signals(parseProfileSignals(asStr(r[2])))
                    .build());
        }
        return InsightsPage.of(content, pg, sz, total);
    }

    @SuppressWarnings("unchecked")
    private List<UserProfileDto.Signal> parseProfileSignals(String metadataJson) {
        List<UserProfileDto.Signal> out = new ArrayList<>();
        if (metadataJson == null || metadataJson.isBlank()) return out;
        try {
            Map<String, Object> meta = objectMapper.readValue(metadataJson, Map.class);
            for (Map.Entry<String, Object> e : meta.entrySet()) {
                String type = e.getKey();
                Object val = e.getValue();
                if (val instanceof Map<?, ?> kv) {
                    for (Map.Entry<?, ?> sub : kv.entrySet()) {
                        out.add(UserProfileDto.Signal.builder()
                                .type(type)
                                .key(String.valueOf(sub.getKey()))
                                .score(sub.getValue() instanceof Number n ? n.doubleValue() : null)
                                .build());
                    }
                } else {
                    out.add(UserProfileDto.Signal.builder()
                            .type(type)
                            .key(String.valueOf(val))
                            .score(null)
                            .build());
                }
            }
        } catch (Exception ex) {
            log.warn("Khong parse duoc metadata profile: {}", ex.getMessage());
        }
        return out;
    }

    // ── 5) Gợi ý theo profile (cb_content_recommendation) ──────────────────────

    @Transactional(readOnly = true)
    public InsightsPage<CbRecommendationDto> getCbRecommendations(Long userId, int page, int size) {
        int sz = safeSize(size);
        int pg = Math.max(page, 0);

        String where = userId != null ? "WHERE c.user_id = :userId " : "";

        Query countQ = em.createNativeQuery(
                "SELECT COUNT(*) FROM cb_content_recommendation c " + where);
        if (userId != null) countQ.setParameter("userId", userId);
        long total = asLong(countQ.getSingleResult());

        Query q = em.createNativeQuery(
                "SELECT c.user_id, u.user_name, "
                        + "       CAST(c.product_ids AS CHAR), CAST(c.similarities AS CHAR), "
                        + "       c.top_k, c.computed_at "
                        + "FROM cb_content_recommendation c "
                        + "LEFT JOIN users u ON u.id = c.user_id "
                        + where
                        + "ORDER BY c.user_id ASC "
                        + "LIMIT :limit OFFSET :offset");
        if (userId != null) q.setParameter("userId", userId);
        q.setParameter("limit", sz);
        q.setParameter("offset", pg * sz);

        List<Object[]> rows = new ArrayList<>();
        for (Object row : q.getResultList()) rows.add((Object[]) row);

        // Gom tất cả product_id để lấy tên 1 lần
        Map<Long, List<Long>> pidsByUser = new LinkedHashMap<>();
        Map<Long, List<Double>> simsByUser = new LinkedHashMap<>();
        List<Long> allPids = new ArrayList<>();
        for (Object[] r : rows) {
            Long uid = asLongOrNull(r[0]);
            List<Long> pids = parseLongArray(asStr(r[2]));
            List<Double> sims = parseDoubleArray(asStr(r[3]));
            pidsByUser.put(uid, pids);
            simsByUser.put(uid, sims);
            allPids.addAll(pids);
        }
        Map<Long, String> nameMap = loadProductNames(allPids);

        List<CbRecommendationDto> content = new ArrayList<>();
        for (Object[] r : rows) {
            Long uid = asLongOrNull(r[0]);
            List<Long> pids = pidsByUser.getOrDefault(uid, List.of());
            List<Double> sims = simsByUser.getOrDefault(uid, List.of());
            List<CbRecommendationDto.Item> items = new ArrayList<>();
            for (int i = 0; i < pids.size(); i++) {
                Long pid = pids.get(i);
                items.add(CbRecommendationDto.Item.builder()
                        .rank(i + 1)
                        .productId(pid)
                        .productName(nameMap.get(pid))
                        .similarity(i < sims.size() ? sims.get(i) : null)
                        .build());
            }
            content.add(CbRecommendationDto.builder()
                    .userId(uid)
                    .userName(asStr(r[1]))
                    .topK(asInt(r[4]))
                    .computedAt(asStr(r[5]))
                    .items(items)
                    .build());
        }
        return InsightsPage.of(content, pg, sz, total);
    }

    private Map<Long, String> loadProductNames(List<Long> ids) {
        Map<Long, String> map = new LinkedHashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        List<Long> distinct = ids.stream().distinct().toList();
        Query q = em.createNativeQuery(
                "SELECT id, product_name FROM products WHERE id IN (:ids)");
        q.setParameter("ids", distinct);
        for (Object row : q.getResultList()) {
            Object[] r = (Object[]) row;
            map.put(asLongOrNull(r[0]), asStr(r[1]));
        }
        return map;
    }

    private List<Long> parseLongArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            Long[] arr = objectMapper.readValue(json, Long[].class);
            return new ArrayList<>(List.of(arr));
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private List<Double> parseDoubleArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            Double[] arr = objectMapper.readValue(json, Double[].class);
            return new ArrayList<>(List.of(arr));
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    // ── Thống kê tương tác người dùng (collector_log) ──────────────────────────

    /**
     * Đếm số lượt theo từng loại sự kiện (event) trong collector_log.
     * @param days số ngày gần đây để lọc; null hoặc &lt;= 0 = toàn bộ thời gian.
     */
    @Transactional(readOnly = true)
    public List<com.ndh.ShopTechnology.dto.response.recommendation.insights.EventStatDto> getEventStats(Integer days) {
        boolean filter = days != null && days > 0;
        String sql = "SELECT event, COUNT(*) AS cnt FROM collector_log "
                + (filter ? "WHERE timestamp >= :start " : "")
                + "GROUP BY event ORDER BY cnt DESC";
        Query q = em.createNativeQuery(sql);
        if (filter) {
            long ms = System.currentTimeMillis() - (long) days * 24L * 60L * 60L * 1000L;
            q.setParameter("start", new java.util.Date(ms));
        }
        List<?> rows = q.getResultList();
        List<com.ndh.ShopTechnology.dto.response.recommendation.insights.EventStatDto> out = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            out.add(com.ndh.ShopTechnology.dto.response.recommendation.insights.EventStatDto.builder()
                    .event(asStr(r[0]))
                    .count(asLong(r[1]))
                    .build());
        }
        return out;
    }
}
