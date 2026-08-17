package com.bor.eboard.checklist.repository;

import com.bor.eboard.checklist.entity.ChecklistInstanceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChecklistInstanceItemRepository extends JpaRepository<ChecklistInstanceItem, UUID> {
    Optional<ChecklistInstanceItem> findByIdAndDeletedFalse(UUID id);
    List<ChecklistInstanceItem> findByChecklistInstanceIdAndActiveTrueAndDeletedFalseOrderBySequenceAsc(UUID instanceId);
    List<ChecklistInstanceItem> findByChecklistInstanceIdAndDeletedFalseOrderBySequenceAsc(UUID instanceId);
}
