package com.bor.eboard.workflow.repository;

import com.bor.eboard.workflow.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {

    Optional<WorkflowStep> findByIdAndDeletedFalse(UUID id);

    List<WorkflowStep> findByWorkflowTemplateIdAndDeletedFalseOrderByStepOrderAsc(UUID templateId);

    Optional<WorkflowStep> findByWorkflowTemplateIdAndStepOrderAndDeletedFalse(
            UUID templateId, Integer stepOrder);

    boolean existsByWorkflowTemplateIdAndStepOrderAndDeletedFalse(
            UUID templateId, Integer stepOrder);
}
