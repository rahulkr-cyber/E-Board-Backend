package com.bor.eboard.workflow.repository;

import com.bor.eboard.workflow.entity.WorkflowTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, UUID> {

    Optional<WorkflowTask> findByIdAndStatus(UUID id, String status);

    List<WorkflowTask> findByWorkflowInstanceIdAndStatus(UUID workflowInstanceId, String status);

    Optional<WorkflowTask> findFirstByFileIdAndStatusOrderByAssignedAtDesc(
            UUID fileId, String status);

    Optional<WorkflowTask> findFirstByLetterIdAndStatusOrderByAssignedAtDesc(
            UUID letterId, String status);

    /**
     * Pending queue: tasks assigned directly to the user, to any of the
     * user's active roles, or to the user's section
     * (08_WORKFLOW_ENGINE.md section 13). Charge-holder tasks are folded in
     * by the service when the Charge module lands (Phase 9).
     */
    @Query("""
            SELECT t FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND (
                    t.assignedToUserId = :userId
                 OR (t.assignedToRoleId IS NOT NULL AND t.assignedToRoleId IN :roleIds)
                 OR (t.assignedToSectionId IS NOT NULL AND t.assignedToSectionId = :sectionId)
              )
              AND (:overdueOnly = FALSE OR (t.dueDate IS NOT NULL AND t.dueDate < :today))
            """)
    Page<WorkflowTask> findMyPending(@Param("userId") UUID userId,
                                     @Param("roleIds") List<UUID> roleIds,
                                     @Param("sectionId") UUID sectionId,
                                     @Param("overdueOnly") boolean overdueOnly,
                                     @Param("today") java.time.LocalDate today,
                                     Pageable pageable);
    
    @Query("""
    	    SELECT t FROM WorkflowTask t
    	    JOIN WorkflowInstance i ON t.workflowInstanceId = i.id
    	    WHERE t.letterId = :letterId
    	      AND t.status = 'PENDING'
    	      AND i.status = 'ACTIVE'
    	    ORDER BY t.assignedAt DESC
    	""")
    	Optional<WorkflowTask> findCurrentTask(UUID letterId);

    /**
     * As {@link #findMyPending} but additionally folds in tasks assigned to
     * users for whom the caller currently holds an active charge
     * (12_BUSINESS_RULES.md section 12 rule 6). {@code chargeUserIds} must be
     * non-empty; callers pass an impossible id as a guard when the caller
     * holds no charge.
     */
    @Query("""
            SELECT t FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND (
                    t.assignedToUserId = :userId
                 OR (t.assignedToRoleId IS NOT NULL AND t.assignedToRoleId IN :roleIds)
                 OR (t.assignedToSectionId IS NOT NULL AND t.assignedToSectionId = :sectionId)
                 OR (t.assignedToUserId IS NOT NULL AND t.assignedToUserId IN :chargeUserIds)
              )
              AND (:overdueOnly = FALSE OR (t.dueDate IS NOT NULL AND t.dueDate < :today))
            """)
    Page<WorkflowTask> findMyPendingWithCharge(@Param("userId") UUID userId,
                                               @Param("roleIds") List<UUID> roleIds,
                                               @Param("sectionId") UUID sectionId,
                                               @Param("chargeUserIds") List<UUID> chargeUserIds,
                                               @Param("overdueOnly") boolean overdueOnly,
                                               @Param("today") java.time.LocalDate today,
                                               Pageable pageable);

    /** Overdue pending tasks for the escalation sweep (Phase 6 scheduler). */
    @Query("""
            SELECT t FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND t.dueDate IS NOT NULL
              AND t.dueDate < :today
            """)
    List<WorkflowTask> findOverdue(@Param("today") java.time.LocalDate today);

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND t.dueDate = :date
            """)
    long countPendingDueOn(@Param("date") java.time.LocalDate date);

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND t.dueDate IS NOT NULL
              AND t.dueDate <= :criticalDueDate
            """)
    long countCriticalOverdue(@Param("criticalDueDate") java.time.LocalDate criticalDueDate);

    // ---- Dashboard aggregates ----

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND (:userId IS NULL OR t.assignedToUserId = :userId)
              AND (:sectionId IS NULL OR t.assignedToSectionId = :sectionId)
            """)
    long countPendingByScope(@Param("userId") UUID userId,
                             @Param("sectionId") UUID sectionId);

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND t.dueDate IS NOT NULL AND t.dueDate < :today
              AND (:userId IS NULL OR t.assignedToUserId = :userId)
              AND (:sectionId IS NULL OR t.assignedToSectionId = :sectionId)
            """)
    long countOverdueByScope(@Param("userId") UUID userId,
                             @Param("sectionId") UUID sectionId,
                             @Param("today") java.time.LocalDate today);

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.letterId IS NOT NULL AND t.status = 'PENDING'
              AND (:userId IS NULL OR t.assignedToUserId = :userId)
              AND (:sectionId IS NULL OR t.assignedToSectionId = :sectionId)
            """)
    long countPendingLettersByScope(@Param("userId") UUID userId, @Param("sectionId") UUID sectionId);

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.letterId IS NOT NULL AND t.status = 'PENDING'
              AND t.dueDate IS NOT NULL AND t.dueDate < :today
              AND (:userId IS NULL OR t.assignedToUserId = :userId)
              AND (:sectionId IS NULL OR t.assignedToSectionId = :sectionId)
            """)
    long countOverdueLettersByScope(@Param("userId") UUID userId, @Param("sectionId") UUID sectionId,
                                    @Param("today") java.time.LocalDate today);


    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND t.assignedToSectionId IN :sectionIds
            """)
    long countPendingBySections(
            @Param("sectionIds") java.util.Collection<UUID> sectionIds);

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.status = 'PENDING'
              AND t.dueDate IS NOT NULL AND t.dueDate < :today
              AND t.assignedToSectionId IN :sectionIds
            """)
    long countOverdueBySections(
            @Param("sectionIds") java.util.Collection<UUID> sectionIds,
            @Param("today") java.time.LocalDate today);

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.letterId IS NOT NULL AND t.status = 'PENDING'
              AND t.assignedToSectionId IN :sectionIds
            """)
    long countPendingLettersBySections(
            @Param("sectionIds") java.util.Collection<UUID> sectionIds);

    @Query("""
            SELECT COUNT(t) FROM WorkflowTask t
            WHERE t.letterId IS NOT NULL AND t.status = 'PENDING'
              AND t.dueDate IS NOT NULL AND t.dueDate < :today
              AND t.assignedToSectionId IN :sectionIds
            """)
    long countOverdueLettersBySections(
            @Param("sectionIds") java.util.Collection<UUID> sectionIds,
            @Param("today") java.time.LocalDate today);
}
