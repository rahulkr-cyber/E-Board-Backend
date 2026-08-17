package com.bor.eboard.workflow.repository;

import com.bor.eboard.workflow.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    Optional<WorkflowInstance> findByFileIdAndStatus(UUID fileId, String status);

    List<WorkflowInstance> findByFileIdOrderByStartedAtDesc(UUID fileId);

    Optional<WorkflowInstance> findByLetterIdAndStatus(UUID letterId, String status);

    List<WorkflowInstance> findByLetterIdOrderByStartedAtDesc(UUID letterId);
}
