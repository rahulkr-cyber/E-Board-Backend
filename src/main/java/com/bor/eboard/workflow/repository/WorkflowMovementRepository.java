package com.bor.eboard.workflow.repository;

import com.bor.eboard.workflow.entity.WorkflowMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowMovementRepository extends JpaRepository<WorkflowMovement, UUID> {

    List<WorkflowMovement> findByFileIdOrderByActionAtAsc(UUID fileId);

    List<WorkflowMovement> findByLetterIdOrderByActionAtAsc(UUID letterId);

    interface LastMovementProjection {
        UUID getWorkflowInstanceId();
        java.time.LocalDateTime getLastMovementAt();
    }

    @Query("""
            SELECT m.workflowInstanceId AS workflowInstanceId, MAX(m.actionAt) AS lastMovementAt
            FROM WorkflowMovement m
            WHERE m.workflowInstanceId IN :instanceIds
            GROUP BY m.workflowInstanceId
            """)
    List<LastMovementProjection> findLastMovementByWorkflowInstanceIds(
            @Param("instanceIds") java.util.Collection<UUID> instanceIds);

    /** Distinct file ids this user has moved (BCR-03 file boxes). */
    @Query("""
            SELECT DISTINCT m.fileId FROM WorkflowMovement m
            WHERE m.fromUserId = :userId
              AND m.fileId IS NOT NULL
            """)
    List<UUID> findFileIdsMovedBy(@Param("userId") UUID userId);

    /** Distinct file ids this user closed (BCR-03 "My Closed" box). */
    @Query("""
            SELECT DISTINCT m.fileId FROM WorkflowMovement m
            WHERE m.fromUserId = :userId
              AND m.action = 'CLOSE'
              AND m.fileId IS NOT NULL
            """)
    List<UUID> findFileIdsClosedBy(@Param("userId") UUID userId);
}
