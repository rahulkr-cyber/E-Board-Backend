package com.bor.eboard.registry.repository;

import com.bor.eboard.registry.entity.DiaryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, UUID> {

    Optional<DiaryEntry> findByIdAndDeletedFalse(UUID id);

    Optional<DiaryEntry> findByDiaryNumberAndDeletedFalse(String diaryNumber);

    @Query("""
    	    SELECT d
    	    FROM DiaryEntry d
    	    WHERE d.deleted = FALSE
    	      AND (
    	            :diaryNumber IS NULL
    	            OR LOWER(d.diaryNumber) LIKE :diaryNumber
    	      )
    	      AND (
    	            :sender IS NULL
    	            OR LOWER(d.senderName) LIKE :sender
    	      )
    	      AND (
    	            :subject IS NULL
    	            OR LOWER(d.subject) LIKE :subject
    	      )
    	      AND (
    	            :status IS NULL
    	            OR d.status = :status
    	      )
    	      AND (
    	            :sectionId IS NULL
    	            OR d.initialSectionId = :sectionId
    	      )
    	      AND d.receivedDate >= :fromDate
    	      AND d.receivedDate <= :toDate
              AND (:checklistFilter = FALSE OR d.id IN :checklistDiaryIds)
    	    """)
    	Page<DiaryEntry> search(
    	        @Param("diaryNumber") String diaryNumber,
    	        @Param("sender") String sender,
    	        @Param("subject") String subject,
    	        @Param("status") String status,
    	        @Param("sectionId") UUID sectionId,
    	        @Param("fromDate") LocalDateTime fromDate,
    	        @Param("toDate") LocalDateTime toDate,
                @Param("checklistFilter") boolean checklistFilter,
                @Param("checklistDiaryIds") java.util.Collection<UUID> checklistDiaryIds,
    	        Pageable pageable);

    @Query("""
            SELECT d FROM DiaryEntry d
            WHERE d.deleted = FALSE
              AND d.receivedDate >= :fromDate
              AND d.receivedDate <= :toDate
            ORDER BY d.diaryYear ASC, d.diarySequence ASC
            """)
    List<DiaryEntry> diaryRegister(@Param("fromDate") LocalDateTime fromDate,
                                   @Param("toDate") LocalDateTime toDate);

    // ---- Dashboard aggregates ----

    @Query("""
            SELECT COUNT(d) FROM DiaryEntry d
            WHERE d.deleted = FALSE
              AND d.receivedDate >= :fromDate AND d.receivedDate <= :toDate
              AND (:sectionId IS NULL OR d.initialSectionId = :sectionId)
            """)
    long countReceivedBetween(@Param("fromDate") LocalDateTime fromDate,
                              @Param("toDate") LocalDateTime toDate,
                              @Param("sectionId") UUID sectionId);


    @Query("""
            SELECT COUNT(d) FROM DiaryEntry d
            WHERE d.deleted = FALSE
              AND d.receivedDate >= :fromDate AND d.receivedDate <= :toDate
              AND d.initialSectionId IN :sectionIds
            """)
    long countReceivedBetweenSections(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("sectionIds") java.util.Collection<UUID> sectionIds);

    @Query("""
            SELECT COUNT(d) FROM DiaryEntry d
            WHERE d.deleted = FALSE
              AND d.status = :status
              AND (:sectionId IS NULL OR d.initialSectionId = :sectionId)
            """)
    long countByStatusAndScope(@Param("status") String status,
                               @Param("sectionId") UUID sectionId);

    @Query("""
            SELECT d FROM DiaryEntry d
            WHERE d.deleted = FALSE
              AND (CAST(:fromDate AS timestamp) IS NULL OR d.receivedDate >= :fromDate)
              AND (CAST(:toDate AS timestamp) IS NULL OR d.receivedDate <= :toDate)
              AND (:sectionId IS NULL OR d.initialSectionId = :sectionId)
              AND (:status IS NULL OR d.status = :status)
            ORDER BY d.diaryYear ASC, d.diarySequence ASC
            """)
    List<DiaryEntry> diaryRegisterFiltered(@Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate,
                                           @Param("sectionId") UUID sectionId,
                                           @Param("status") String status);
}
