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
public class UserAddressEntity extends BaseEntity {

    public static final String COL_USER_ID      = "user_id";
    public static final String COL_ADDRESS_LINE = "address_line";
    public static final String COL_CITY         = "city";
    public static final String COL_STATE        = "state";
    public static final String COL_COUNTRY      = "country";
    public static final String COL_ZIP_CODE     = "zip_code";
    public static final String COL_DEFAULT      = "is_default";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_USER_ID, nullable = false)
    private UserEntity user;

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
    private Boolean isDefault = false;

}
