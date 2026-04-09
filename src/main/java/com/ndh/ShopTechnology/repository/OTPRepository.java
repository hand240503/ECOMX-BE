package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.otp.OTPEntity;
import com.ndh.ShopTechnology.entities.otp.OTPPurpose;
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
public interface OTPRepository extends JpaRepository<OTPEntity, Long> {

    Optional<OTPEntity> findByLoginAndPurpose(String login, OTPPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OTPEntity o WHERE o.login = :login AND o.purpose = :purpose")
    Optional<OTPEntity> findByLoginAndPurposeForUpdate(@Param("login") String login, @Param("purpose") OTPPurpose purpose);

    void deleteByLoginAndPurpose(String login, OTPPurpose purpose);

    @Modifying
    @Query("DELETE FROM OTPEntity o WHERE o.expiredAt < :now")
    void deleteExpiredOTPs(@Param("now") LocalDateTime now);
}