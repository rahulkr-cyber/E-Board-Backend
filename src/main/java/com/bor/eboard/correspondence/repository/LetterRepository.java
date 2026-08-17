package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.Letter;
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
public interface LetterRepository extends JpaRepository<Letter, UUID> {

    Optional<Letter> findByIdAndDeletedFalse(UUID id);

    List<Letter> findByDiaryEntryIdAndDeletedFalse(UUID diaryEntryId);

    List<Letter> findByFileIdAndDeletedFalseOrderByCreatedAtAsc(UUID fileId);

    List<Letter> findByFileIdInAndDeletedFalseOrderByCreatedAtAsc(java.util.Collection<UUID> fileIds);

    // =================================================================
    // BCR-03 Part 16: ownership-scoped letter boxes. Users never see all
    // letters by default; a new letter is visible only to its creator.
    // =================================================================

    /** My Draft Letters: created by me, not yet marked. */
    @Query("""
            SELECT l FROM Letter l
            WHERE l.deleted = FALSE
              AND l.createdBy = :userId
              AND l.currentStatus = 'DRAFT'
            """)
    Page<Letter> boxDrafts(@Param("userId") UUID userId, Pageable pageable);

    /** My Inbox Letters: currently assigned to me and live. */
    @Query("""
            SELECT l FROM Letter l
            WHERE l.deleted = FALSE
              AND l.currentOwnerId = :userId
              AND l.currentStatus NOT IN ('DRAFT', 'CLOSED', 'ARCHIVED')
            """)
    Page<Letter> boxInbox(@Param("userId") UUID userId, Pageable pageable);

    /** My Sent Letters: marked onward by me and no longer held by me. */
    @Query("""
            SELECT l FROM Letter l
            WHERE l.deleted = FALSE
              AND l.id IN :movedLetterIds
              AND l.currentOwnerId <> :userId
            """)
    Page<Letter> boxSent(@Param("userId") UUID userId,
                         @Param("movedLetterIds") List<UUID> movedLetterIds,
                         Pageable pageable);

    /** My Returned Letters. */
    @Query("""
            SELECT l FROM Letter l
            WHERE l.deleted = FALSE
              AND l.currentOwnerId = :userId
              AND l.currentStatus = 'RETURNED'
            """)
    Page<Letter> boxReturned(@Param("userId") UUID userId, Pageable pageable);

    /** My Closed Letters. */
    @Query("""
            SELECT l FROM Letter l
            WHERE l.deleted = FALSE
              AND l.currentOwnerId = :userId
              AND l.currentStatus IN ('CLOSED', 'ARCHIVED')
            """)
    Page<Letter> boxClosed(@Param("userId") UUID userId, Pageable pageable);

    /** BCR-03 Part 9: dispatch target picker — search letters by number or subject. */
    @Query("""
            SELECT l FROM Letter l
            WHERE l.deleted = FALSE
              AND (LOWER(COALESCE(l.letterNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(l.referenceNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(l.subject) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY l.createdAt DESC
            """)
    List<Letter> searchForDispatchTarget(@Param("query") String query, Pageable pageable);

    /**
     * General letter search (LETTER_VIEW). Distinct from the five personal
     * boxes: this finds a letter by its identifiers regardless of who holds it,
     * which is what a clerk needs when a citizen quotes a letter number.
     * Drafts are excluded — an unmarked letter is private to its creator.
     */
    @Query("""
    		SELECT l
    		FROM Letter l
    		WHERE l.deleted = FALSE
    		AND l.currentStatus <> 'DRAFT'
    		AND (
    		    :letterNumber IS NULL
    		    OR LOWER(COALESCE(l.letterNumber,'')) LIKE :letterNumber
    		)
    		AND (
    		    :subject IS NULL
    		    OR LOWER(l.subject) LIKE :subject
    		)
    		AND (:direction IS NULL OR l.letterDirection = :direction)
    		AND (:letterType IS NULL OR l.letterType = :letterType)
    		AND (:status IS NULL OR l.currentStatus = :status)
    		AND (:sectionId IS NULL OR l.currentSectionId = :sectionId)
    		AND (:fileId IS NULL OR l.fileId = :fileId)
            AND (:checklistFilter = FALSE OR l.id IN :checklistLetterIds)
    		""")
    Page<Letter> search(@Param("letterNumber") String letterNumber,
                        @Param("subject") String subject,
                        @Param("direction") String direction,
                        @Param("letterType") String letterType,
                        @Param("status") String status,
                        @Param("sectionId") UUID sectionId,
                        @Param("fileId") UUID fileId,
                        @Param("checklistFilter") boolean checklistFilter,
                        @Param("checklistLetterIds") java.util.Collection<UUID> checklistLetterIds,
                        Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(l) FROM Letter l
            WHERE l.deleted = FALSE
              AND l.diaryEntryId IS NOT NULL
              AND (:ownerId IS NULL OR l.currentOwnerId = :ownerId)
              AND (:sectionId IS NULL OR l.currentSectionId = :sectionId)
              AND ((:disposed = TRUE AND l.currentStatus IN ('CLOSED','ARCHIVED'))
                OR (:disposed = FALSE AND l.currentStatus NOT IN ('CLOSED','ARCHIVED')))
            """)
    long countDiaryLettersByScope(
            @org.springframework.data.repository.query.Param("ownerId") UUID ownerId,
            @org.springframework.data.repository.query.Param("sectionId") UUID sectionId,
            @org.springframework.data.repository.query.Param("disposed") boolean disposed);


    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(l) FROM Letter l
            WHERE l.deleted = FALSE
              AND l.diaryEntryId IS NOT NULL
              AND l.currentSectionId IN :sectionIds
              AND ((:disposed = TRUE AND l.currentStatus IN ('CLOSED','ARCHIVED'))
                OR (:disposed = FALSE AND l.currentStatus NOT IN ('CLOSED','ARCHIVED')))
            """)
    long countDiaryLettersBySections(
            @org.springframework.data.repository.query.Param("sectionIds")
            java.util.Collection<UUID> sectionIds,
            @org.springframework.data.repository.query.Param("disposed") boolean disposed);
}
