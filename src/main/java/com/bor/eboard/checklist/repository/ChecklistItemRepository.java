package com.bor.eboard.checklist.repository;

import com.bor.eboard.checklist.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {
    Optional<ChecklistItem> findByIdAndDeletedFalse(UUID id);
    List<ChecklistItem> findByChecklistTemplateIdAndDeletedFalseOrderBySequenceAsc(UUID checklistTemplateId);
    List<ChecklistItem> findByChecklistTemplateIdAndActiveTrueAndDeletedFalseOrderBySequenceAsc(UUID checklistTemplateId);
}
