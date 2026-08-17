package com.bor.eboard.checklist.repository;

import com.bor.eboard.checklist.entity.LetterCategoryChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LetterCategoryChecklistRepository extends JpaRepository<LetterCategoryChecklist, UUID> {
    Optional<LetterCategoryChecklist> findByCategoryIdAndDeletedFalse(UUID categoryId);
    Optional<LetterCategoryChecklist> findByCategoryIdAndActiveTrueAndDeletedFalse(UUID categoryId);
    List<LetterCategoryChecklist> findByDeletedFalseOrderByCreatedAtDesc();
}
