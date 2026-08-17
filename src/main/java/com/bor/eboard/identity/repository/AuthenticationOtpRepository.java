package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.AuthenticationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationOtpRepository extends JpaRepository<AuthenticationOtp, UUID> {
    Optional<AuthenticationOtp> findByIdAndPurpose(UUID id, String purpose);
    Optional<AuthenticationOtp> findTopByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, String purpose);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthenticationOtp> findByResetTokenHashAndPurposeAndVerifiedTrueAndConsumedFalse(
            String resetTokenHash, String purpose);

    @Modifying
    @Query("UPDATE AuthenticationOtp o SET o.consumed = TRUE WHERE o.userId = :userId AND o.purpose = :purpose AND o.consumed = FALSE")
    int consumeActive(@Param("userId") UUID userId, @Param("purpose") String purpose);
}
