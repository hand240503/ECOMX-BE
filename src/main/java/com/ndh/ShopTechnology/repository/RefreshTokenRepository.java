package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.token.RefreshTokenEntity;
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
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.tokenId = :tokenId")
    Optional<RefreshTokenEntity> findByTokenIdForUpdate(@Param("tokenId") String tokenId);

    Optional<RefreshTokenEntity> findByUserAndRevokedFalse(UserEntity user);

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true, rt.revokedAt = :revokedAt, rt.revokedReason = :reason WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllUserTokens(@Param("user") UserEntity user, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true, rt.revokedAt = :revokedAt, rt.revokedReason = :reason WHERE rt.familyId = :familyId AND rt.revoked = false")
    void revokeTokenFamily(@Param("familyId") String familyId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiryDate < :date")
    void deleteExpiredTokens(LocalDateTime date);
}