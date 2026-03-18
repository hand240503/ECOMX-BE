package com.ndh.ShopTechnology.entities.token;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "token_id", nullable = false, unique = true, length = 36)
    private String tokenId;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "parent_token_id", length = 36)
    private String parentTokenId;

    @Column(name = "replaced_by_token_id", length = 36)
    private String replacedByTokenId;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime revokedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(name = "revoked_reason", length = 64)
    private String revokedReason;

    // Helper method
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}