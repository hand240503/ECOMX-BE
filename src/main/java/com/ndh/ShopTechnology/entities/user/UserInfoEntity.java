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

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "avatar_public_id", length = 255)
    private String avatarPublicId;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "email")
    private String email;

    @Column(name = "info_01", length = 500)
    private String info01;

    @Column(name = "info_02", length = 500)
    private String info02;

    @Column(name = "info_03", length = 500)
    private String info03;

    @Column(name = "info_04", length = 500)
    private String info04;

    @Transient
    public boolean hasManager() {
        return managerId != null;
    }
}