package com.ndh.ShopTechnology.entities.log;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "collector_log")
public class CollectorLogEntity extends BaseEntity {

    public static final String COL_EVENT = "event";
    public static final String COL_SESSION_ID = "session_id";
    public static final String COL_DEVICE_TYPE = "device_type";
    public static final String COL_PLATFORM = "platform";
    public static final String COL_METADATA = "metadata";
    public static final String COL_IP_ADDRESS = "ip_address";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_PRODUCT_ID = "product_id";
    public static final String COL_USER_ID = "user_id";

    @Column(name = COL_EVENT, nullable = false)
    private String event;

    @Column(name = COL_SESSION_ID, nullable = true)
    private String sessionId;

    @Column(name = COL_DEVICE_TYPE, nullable = true)
    private String deviceType;

    @Column(name = COL_PLATFORM, nullable = true)
    private String platform;

    @Column(name = COL_METADATA, columnDefinition = "TEXT", nullable = true)
    private String metadata;

    @Column(name = COL_IP_ADDRESS, nullable = true)
    private String ipAddress;

    @Column(name = COL_TIMESTAMP, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PRODUCT_ID, nullable = true)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_USER_ID, nullable = true)
    private UserEntity user;
}
