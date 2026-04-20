package com.ndh.ShopTechnology.entities.recommendation;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "item_similarity", indexes = {
                @Index(name = "idx_source", columnList = "source"),
                @Index(name = "idx_target", columnList = "target"),
                @Index(name = "idx_source_algo_rank", columnList = "source,algorithm,rank_pos"),
                @Index(name = "idx_algo", columnList = "algorithm")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uk_src_tgt_algo", columnNames = { "source", "target", "algorithm" })
})
public class ItemSimilarityEntity {

        public static final String COL_SOURCE = "source";
        public static final String COL_TARGET = "target";
        public static final String COL_ALGORITHM = "algorithm";
        public static final String COL_SIMILARITY = "similarity";
        public static final String COL_RANK_POS = "rank_pos";
        public static final String COL_CREATED_DATE = "created_date";
        public static final String COL_UPDATED_AT = "updated_at";

        public static final String ALGO_CF_COSINE = "cf_cosine";
        public static final String ALGO_CONTENT_TFIDF = "content_tfidf";

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = COL_SOURCE, nullable = false)
        private Integer source;

        @Column(name = COL_TARGET, nullable = false)
        private Integer target;

        @Column(name = COL_ALGORITHM, nullable = false, length = 32)
        private String algorithm;

        @Column(name = COL_SIMILARITY, nullable = false, precision = 10, scale = 7)
        private BigDecimal similarity;

        @Column(name = COL_RANK_POS, nullable = false)
        private Short rankPos;

        @Column(name = COL_CREATED_DATE, insertable = false, updatable = false)
        @Temporal(TemporalType.TIMESTAMP)
        private Date createdDate;

        @Column(name = COL_UPDATED_AT, nullable = false)
        @Temporal(TemporalType.TIMESTAMP)
        private Date updatedAt;
}