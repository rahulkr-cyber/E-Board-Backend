package com.bor.eboard.notification.repository;

import com.bor.eboard.notification.entity.TaskEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskEscalationRepository extends JpaRepository<TaskEscalation, UUID> {

    boolean existsByTaskIdAndEscalationLevel(UUID taskId, String escalationLevel);

    java.util.Optional<TaskEscalation> findFirstByTaskIdOrderByDaysAfterDueDesc(UUID taskId);

    java.util.List<TaskEscalation> findByTaskIdIn(java.util.Collection<UUID> taskIds);

    java.util.List<TaskEscalation> findByTaskIdOrderByDaysAfterDueAsc(UUID taskId);
}
