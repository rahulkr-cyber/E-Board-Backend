package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revoked = TRUE, rt.revokedAt = :now
            WHERE rt.userId = :userId AND rt.revoked = FALSE
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
