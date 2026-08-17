package com.bor.eboard.charge.repository;

import com.bor.eboard.charge.entity.ChargeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChargeAssignmentRepository extends JpaRepository<ChargeAssignment, UUID> {

    Optional<ChargeAssignment> findByIdAndDeletedFalse(UUID id);

    List<ChargeAssignment> findByDeletedFalseAndStatusOrderByEffectiveFromDesc(String status);

    List<ChargeAssignment> findByToUserIdAndDeletedFalseOrderByEffectiveFromDesc(UUID toUserId);

    List<ChargeAssignment> findByFromUserIdAndDeletedFalseOrderByEffectiveFromDesc(UUID fromUserId);

    /**
     * The from-user-ids for which the given user currently holds an ACTIVE
     * charge that is within its effective window. Drives workflow queue
     * access (12_BUSINESS_RULES.md section 12 rule 6).
     */
    @Query("""
            SELECT c.fromUserId FROM ChargeAssignment c
            WHERE c.deleted = FALSE
              AND c.status = 'ACTIVE'
              AND c.toUserId = :toUserId
              AND c.effectiveFrom <= :now
              AND (c.effectiveTo IS NULL OR c.effectiveTo >= :now)
            """)
    List<UUID> findActiveGrantorUserIds(@Param("toUserId") UUID toUserId,
                                        @Param("now") LocalDateTime now);

    /** ACTIVE charges whose effective_to has passed — candidates for auto-expiry. */
    @Query("""
            SELECT c FROM ChargeAssignment c
            WHERE c.deleted = FALSE
              AND c.status = 'ACTIVE'
              AND c.effectiveTo IS NOT NULL
              AND c.effectiveTo < :now
            """)
    List<ChargeAssignment> findExpirable(@Param("now") LocalDateTime now);
}
