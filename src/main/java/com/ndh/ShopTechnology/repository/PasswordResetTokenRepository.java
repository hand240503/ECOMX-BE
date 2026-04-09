package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.token.PasswordResetTokenEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PasswordResetTokenEntity t WHERE t.tokenHash = :tokenHash")
    Optional<PasswordResetTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetTokenEntity t SET t.revoked = true WHERE t.user = :user AND t.revoked = false AND t.usedAt IS NULL")
    void revokeAllActiveByUser(@Param("user") UserEntity user);

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiryAt < :now OR t.usedAt IS NOT NULL OR t.revoked = true")
    void deleteExpiredOrInactive(@Param("now") LocalDateTime now);
}
