package com.ndh.ShopTechnology.entities.user;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "user_address")
@Table(name = "user_address")
public class UserAddressEntity extends BaseEntity {

    public static final String COL_USER_ID = "user_id";
    public static final String COL_ADDRESS_TYPE = "address_type";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_DISTANCE_TO_WAREHOUSE = "distance_to_warehouse_meters";
    public static final String COL_SHIPPING_FEE_VND = "shipping_fee_vnd";
    public static final String COL_ADDRESS_LINE = "address_line";
    public static final String COL_CITY = "city";
    public static final String COL_STATE = "state";
    public static final String COL_COUNTRY = "country";
    public static final String COL_ZIP_CODE = "zip_code";
    public static final String COL_DEFAULT = "is_default";

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = COL_USER_ID, nullable = true)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = COL_ADDRESS_TYPE, nullable = false, length = 32)
    @Builder.Default
    private AddressType addressType = AddressType.USER;

    @Column(name = COL_LATITUDE)
    private Double latitude;

    @Column(name = COL_LONGITUDE)
    private Double longitude;

    /** Quãng đường lái xe (OSRM) từ điểm này tới kho; với WAREHOUSE thường là 0. */
    @Column(name = COL_DISTANCE_TO_WAREHOUSE)
    private Double distanceToWarehouseMeters;

    /** Phí ship (VND) lưu cùng lúc cập nhật khoảng cách; đặt hàng ưu tiên cột này. */
    @Column(name = COL_SHIPPING_FEE_VND)
    private Long shippingFeeVnd;

    @Column(name = COL_ADDRESS_LINE, nullable = false)
    private String addressLine;

    @Column(name = COL_CITY, nullable = false)
    private String city;

    @Column(name = COL_STATE, nullable = true)
    private String state;

    @Column(name = COL_COUNTRY, nullable = false)
    private String country;

    @Column(name = COL_ZIP_CODE, nullable = true)
    private String zipCode;

    @Column(name = COL_DEFAULT, nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

}
