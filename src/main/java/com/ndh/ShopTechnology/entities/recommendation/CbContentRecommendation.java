package com.ndh.ShopTechnology.entities.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "cb_content_recommendation",
        indexes = {
                @Index(name = "idx_cb_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cb_user", columnNames = "user_id")
        }
)
public class CbContentRecommendation {

    public static final String COL_USER_ID      = "user_id";
    public static final String COL_PRODUCT_IDS  = "product_ids";
    public static final String COL_SIMILARITIES = "similarities";
    public static final String COL_TOP_K        = "top_k";
    public static final String COL_EXCLUDE_SEEN = "exclude_seen";
    public static final String COL_COMPUTED_AT  = "computed_at";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = COL_USER_ID, nullable = false, unique = true)
    private Long userId;

    /**
     * Optional navigation to {@code users}; same column as {@link #userId}, read-only for JPA so
     * writes still go through {@code userId}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = COL_USER_ID,
            referencedColumnName = "id",
            nullable = false,
            insertable = false,
            updatable = false)
    @JsonIgnore
    private UserEntity user;

    /** Mảng product_id đã sort theo rank (rank 1 = phần tử [0]). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = COL_PRODUCT_IDS, nullable = false, columnDefinition = "json")
    private List<Long> productIds;

    /** Mảng similarity tương ứng (cùng index với productIds). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = COL_SIMILARITIES, nullable = false, columnDefinition = "json")
    private List<Double> similarities;

    @Column(name = COL_TOP_K, nullable = false)
    private Integer topK;

    @Column(name = COL_EXCLUDE_SEEN, nullable = false)
    private Boolean excludeSeen;

    @Column(name = COL_COMPUTED_AT, nullable = false)
    private LocalDateTime computedAt;
}