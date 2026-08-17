package com.bor.eboard.checklist.repository;

import com.bor.eboard.checklist.entity.ChecklistInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChecklistInstanceRepository extends JpaRepository<ChecklistInstance, UUID> {
    Optional<ChecklistInstance> findByIdAndDeletedFalse(UUID id);
    Optional<ChecklistInstance> findByDiaryEntryIdAndDeletedFalse(UUID diaryEntryId);
    Optional<ChecklistInstance> findByLetterIdAndDeletedFalse(UUID letterId);
    List<ChecklistInstance> findByDeletedFalseOrderByCreatedAtDesc();
    boolean existsByChecklistTemplateIdAndDeletedFalse(UUID templateId);

    @Query("""
            SELECT ci FROM ChecklistInstance ci
            WHERE ci.deleted = FALSE
              AND ((ci.letterId IS NOT NULL AND EXISTS (
                    SELECT l.id FROM Letter l
                    WHERE l.id = ci.letterId AND l.deleted = FALSE
                      AND l.currentOwnerId = :ownerId))
                OR (ci.letterId IS NULL AND ci.diaryEntryId IS NOT NULL AND EXISTS (
                    SELECT d.id FROM DiaryEntry d
                    WHERE d.id = ci.diaryEntryId AND d.deleted = FALSE
                      AND d.receivedBy = :ownerId)))
            ORDER BY ci.createdAt DESC
            """)
    List<ChecklistInstance> findForOwner(@Param("ownerId") UUID ownerId);

    @Query("""
            SELECT ci FROM ChecklistInstance ci
            WHERE ci.deleted = FALSE
              AND ((ci.letterId IS NOT NULL AND EXISTS (
                    SELECT l.id FROM Letter l
                    WHERE l.id = ci.letterId AND l.deleted = FALSE
                      AND l.currentSectionId IN :sectionIds))
                OR (ci.letterId IS NULL AND ci.diaryEntryId IS NOT NULL AND EXISTS (
                    SELECT d.id FROM DiaryEntry d
                    WHERE d.id = ci.diaryEntryId AND d.deleted = FALSE
                      AND d.initialSectionId IN :sectionIds)))
            ORDER BY ci.createdAt DESC
            """)
    List<ChecklistInstance> findForSections(@Param("sectionIds") Collection<UUID> sectionIds);
}
