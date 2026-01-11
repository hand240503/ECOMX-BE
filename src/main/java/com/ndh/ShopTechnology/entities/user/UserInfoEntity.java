package com.ndh.ShopTechnology.entities.user;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user_info")
public class UserInfoEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "manager_id")
    private Long managerId;

    // Các trường thông tin tùy biến
    @Column(name = "info_01", length = 500)
    private String info01;

    @Column(name = "info_02", length = 500)
    private String info02;

    @Column(name = "info_03", length = 500)
    private String info03;

    @Column(name = "info_04", length = 500)
    private String info04;

    // Helper method để lấy full name
    @Transient
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    // Helper method để kiểm tra có manager không
    @Transient
    public boolean hasManager() {
        return managerId != null;
    }
}