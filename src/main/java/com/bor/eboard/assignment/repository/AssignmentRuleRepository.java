package com.bor.eboard.assignment.repository;

import com.bor.eboard.assignment.entity.AssignmentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, UUID> {

    Optional<AssignmentRule> findByIdAndDeletedFalse(UUID id);

    List<AssignmentRule> findByDeletedFalseOrderByCreatedAtDesc();

    /** All active rules that apply to any of the caller's roles. */
    @Query("""
            SELECT r FROM AssignmentRule r
            WHERE r.deleted = FALSE
              AND r.active = TRUE
              AND r.fromRoleId IN :fromRoleIds
            """)
    List<AssignmentRule> findActiveForFromRoles(@Param("fromRoleIds") List<UUID> fromRoleIds);

    /** Active rules for a specific from-role/to-role pair. */
    @Query("""
            SELECT r FROM AssignmentRule r
            WHERE r.deleted = FALSE
              AND r.active = TRUE
              AND r.fromRoleId IN :fromRoleIds
              AND r.toRoleId = :toRoleId
            """)
    List<AssignmentRule> findActiveForPair(@Param("fromRoleIds") List<UUID> fromRoleIds,
                                           @Param("toRoleId") UUID toRoleId);
}
