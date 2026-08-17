package com.bor.eboard.workflow.repository;

import com.bor.eboard.workflow.entity.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

    Optional<WorkflowTemplate> findByIdAndDeletedFalse(UUID id);

    boolean existsByCodeAndDeletedFalse(String code);

    List<WorkflowTemplate> findByDeletedFalseAndActiveTrueOrderByNameAsc();

    List<WorkflowTemplate> findByDeletedFalseOrderByNameAsc();

    /**
     * Candidate templates for resolution: active, not deleted, and whose
     * selection dimensions are either null (wildcard) or match the file.
     * Ranking by specificity is done in the resolver.
     */
    @Query("""
            SELECT t FROM WorkflowTemplate t
            WHERE t.deleted = FALSE AND t.active = TRUE
              AND (t.categoryId IS NULL OR t.categoryId = :categoryId)
              AND (t.departmentId IS NULL OR t.departmentId = :departmentId)
              AND (t.sectionId IS NULL OR t.sectionId = :sectionId)
              AND (t.priorityId IS NULL OR t.priorityId = :priorityId)
            """)
    List<WorkflowTemplate> findCandidates(@Param("categoryId") UUID categoryId,
                                          @Param("departmentId") UUID departmentId,
                                          @Param("sectionId") UUID sectionId,
                                          @Param("priorityId") UUID priorityId);
}
