package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Optional<FileEntity> findByIdAndDeletedFalse(UUID id);

    @Query("""
    	    SELECT f
    	    FROM FileEntity f
    	    WHERE f.deleted = FALSE
    	      AND (
    	            :fileNumber IS NULL
    	            OR LOWER(f.fileNumber) LIKE :fileNumber
    	      )
    	      AND (
    	            :status IS NULL
    	            OR f.currentStatus = :status
    	      )
    	      AND (
    	            :sectionId IS NULL
    	            OR f.currentSectionId = :sectionId
    	      )
    	      AND (
    	            :ownerId IS NULL
    	            OR f.currentOwnerId = :ownerId
    	      )
    	    """)
    	Page<FileEntity> search(
    	        @Param("fileNumber") String fileNumber,
    	        @Param("status") String status,
    	        @Param("sectionId") UUID sectionId,
    	        @Param("ownerId") UUID ownerId,
    	        Pageable pageable);

    // ---- Dashboard aggregates (scope: null owner/section = global) ----

    @Query("""
            SELECT COUNT(f) FROM FileEntity f
            WHERE f.deleted = FALSE
              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
              AND (:status IS NULL OR f.currentStatus = :status)
            """)
    long countByScopeAndStatus(@Param("ownerId") UUID ownerId,
                               @Param("sectionId") UUID sectionId,
                               @Param("status") String status);

    @Query("""
            SELECT COUNT(f) FROM FileEntity f
            WHERE f.deleted = FALSE
              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
              AND f.currentStatus NOT IN ('CLOSED', 'ARCHIVED', 'REJECTED')
            """)
    long countOpenByScope(@Param("ownerId") UUID ownerId,
                          @Param("sectionId") UUID sectionId);

    @Query("""
            SELECT COUNT(f) FROM FileEntity f
            WHERE f.deleted = FALSE
              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
              AND f.currentStatus = 'CLOSED'
              AND f.closedDate >= :fromDate
            """)
    long countDisposedSince(@Param("ownerId") UUID ownerId,
                            @Param("sectionId") UUID sectionId,
                            @Param("fromDate") java.time.LocalDate fromDate);

//    @Query("""
//            SELECT COUNT(f) FROM FileEntity f
//            WHERE f.deleted = FALSE
//              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
//              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
//              AND f.currentStatus IN ('CLOSED', 'ARCHIVED')
//              AND (:fromDate IS NULL OR f.closedDate >= :fromDate)
//              AND (:toDate IS NULL OR f.closedDate <= :toDate)
//            """)
//    long countDisposedBetween(@Param("ownerId") UUID ownerId,
//                              @Param("sectionId") UUID sectionId,
//                              @Param("fromDate") java.time.LocalDate fromDate,
//                              @Param("toDate") java.time.LocalDate toDate);

    
    @Query("""
    		SELECT COUNT(f)
    		FROM FileEntity f
    		WHERE f.deleted = FALSE
    		  AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
    		  AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
    		  AND f.currentStatus IN ('CLOSED','ARCHIVED')
    		  AND (CAST(:fromDate AS date) IS NULL OR f.closedDate >= :fromDate)
    		  AND (CAST(:toDate AS date) IS NULL OR f.closedDate <= :toDate)
    		""")
    		long countDisposedBetween(
    		        UUID ownerId,
    		        UUID sectionId,
    		        LocalDate fromDate,
    		        LocalDate toDate);
    
//    @Query("""
//            SELECT COUNT(f) FROM FileEntity f
//            WHERE f.deleted = FALSE
//              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
//              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
//              AND (:fromDate IS NULL OR f.openedDate >= :fromDate)
//              AND (:toDate IS NULL OR f.openedDate <= :toDate)
//            """)
//    long countOpenedBetween(@Param("ownerId") UUID ownerId,
//                            @Param("sectionId") UUID sectionId,
//                            @Param("fromDate") java.time.LocalDate fromDate,
//                            @Param("toDate") java.time.LocalDate toDate);
    
    @Query("""
    	    SELECT COUNT(f)
    	    FROM FileEntity f
    	    WHERE f.deleted = FALSE
    	      AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
    	      AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
    	      AND (CAST(:fromDate AS date) IS NULL OR f.openedDate >= :fromDate)
    	      AND (CAST(:toDate AS date) IS NULL OR f.openedDate <= :toDate)
    	""")
    	long countOpenedBetween(
    	        @Param("ownerId") UUID ownerId,
    	        @Param("sectionId") UUID sectionId,
    	        @Param("fromDate") LocalDate fromDate,
    	        @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COUNT(f) FROM FileEntity f
            WHERE f.deleted = FALSE
              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
              AND f.openedDate = :date
            """)
    long countOpenedOn(@Param("ownerId") UUID ownerId,
                       @Param("sectionId") UUID sectionId,
                       @Param("date") java.time.LocalDate date);

    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
            ORDER BY f.createdAt DESC
            """)
    java.util.List<FileEntity> recentByScope(@Param("ownerId") UUID ownerId,
                                             @Param("sectionId") UUID sectionId,
                                             Pageable pageable);

//    @Query("""
//            SELECT f FROM FileEntity f
//            WHERE f.deleted = FALSE
//              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
//              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
//              AND (:fromDate IS NULL OR f.openedDate >= :fromDate)
//              AND (:toDate IS NULL OR f.openedDate <= :toDate)
//            ORDER BY f.createdAt DESC
//            """)
//    java.util.List<FileEntity> recentByScopeAndDateRange(
//            @Param("ownerId") UUID ownerId,
//            @Param("sectionId") UUID sectionId,
//            @Param("fromDate") java.time.LocalDate fromDate,
//            @Param("toDate") java.time.LocalDate toDate,
//            Pageable pageable);

    
    @Query("""
    	    SELECT f
    	    FROM FileEntity f
    	    WHERE f.deleted = FALSE
    	      AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
    	      AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
    	      AND (CAST(:fromDate AS date) IS NULL OR f.openedDate >= :fromDate)
    	      AND (CAST(:toDate AS date) IS NULL OR f.openedDate <= :toDate)
    	    ORDER BY f.createdAt DESC
    	""")
    	List<FileEntity> recentByScopeAndDateRange(
    	        @Param("ownerId") UUID ownerId,
    	        @Param("sectionId") UUID sectionId,
    	        @Param("fromDate") LocalDate fromDate,
    	        @Param("toDate") LocalDate toDate,
    	        Pageable pageable);

    // ---- Dashboard aggregates for a department's section set ----

    @Query("""
            SELECT COUNT(f) FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentSectionId IN :sectionIds
              AND (:status IS NULL OR f.currentStatus = :status)
            """)
    long countBySectionIdsAndStatus(
            @Param("sectionIds") java.util.Collection<UUID> sectionIds,
            @Param("status") String status);

    @Query("""
            SELECT COUNT(f) FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentSectionId IN :sectionIds
              AND f.currentStatus = 'CLOSED'
              AND f.closedDate >= :fromDate
            """)
    long countDisposedSinceBySectionIds(
            @Param("sectionIds") java.util.Collection<UUID> sectionIds,
            @Param("fromDate") java.time.LocalDate fromDate);

    @Query("""
            SELECT COUNT(f) FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentSectionId IN :sectionIds
              AND f.openedDate = :date
            """)
    long countOpenedOnBySectionIds(
            @Param("sectionIds") java.util.Collection<UUID> sectionIds,
            @Param("date") java.time.LocalDate date);

    @Query("""
    		SELECT COUNT(f)
    		FROM FileEntity f
    		WHERE f.deleted = FALSE
    		AND f.currentSectionId IN :sectionIds
    		AND (CAST(:fromDate AS date) IS NULL OR f.openedDate >= :fromDate)
    		AND (CAST(:toDate AS date) IS NULL OR f.openedDate <= :toDate)
    		""")
    long countOpenedBetweenBySectionIds(
            @Param("sectionIds") java.util.Collection<UUID> sectionIds,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate);

    @Query("""
    	    SELECT COUNT(f)
    	    FROM FileEntity f
    	    WHERE f.deleted = FALSE
    	      AND f.currentSectionId IN :sectionIds
    	      AND f.currentStatus IN ('CLOSED','ARCHIVED')
    	      AND (CAST(:fromDate AS date) IS NULL OR f.closedDate >= :fromDate)
    	      AND (CAST(:toDate AS date) IS NULL OR f.closedDate <= :toDate)
    	""")
    	long countDisposedBetweenBySectionIds(
    	        @Param("sectionIds") Collection<UUID> sectionIds,
    	        @Param("fromDate") LocalDate fromDate,
    	        @Param("toDate") LocalDate toDate);

//    @Query("""
//            SELECT f FROM FileEntity f
//            WHERE f.deleted = FALSE
//              AND f.currentSectionId IN :sectionIds
//              AND (:fromDate IS NULL OR f.openedDate >= :fromDate)
//              AND (:toDate IS NULL OR f.openedDate <= :toDate)
//            ORDER BY f.createdAt DESC
//            """)
//    java.util.List<FileEntity> recentBySectionIdsAndDateRange(
//            @Param("sectionIds") java.util.Collection<UUID> sectionIds,
//            @Param("fromDate") java.time.LocalDate fromDate,
//            @Param("toDate") java.time.LocalDate toDate,
//            Pageable pageable);
    
    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentSectionId IN :sectionIds
              AND f.openedDate BETWEEN :fromDate AND :toDate
            ORDER BY f.createdAt DESC
            """)
    List<FileEntity> recentBySectionIdsAndDateRange(
            @Param("sectionIds") Collection<UUID> sectionIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    // ---- Report queries ----

    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentStatus NOT IN ('CLOSED', 'ARCHIVED', 'REJECTED')
              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
              AND (:categoryId IS NULL OR f.categoryId = :categoryId)
              AND (:priorityId IS NULL OR f.priorityId = :priorityId)
            ORDER BY f.openedDate ASC
            """)
    java.util.List<FileEntity> pendingFiles(@Param("sectionId") UUID sectionId,
                                            @Param("ownerId") UUID ownerId,
                                            @Param("categoryId") UUID categoryId,
                                            @Param("priorityId") UUID priorityId);

//    @Query("""
//            SELECT f FROM FileEntity f
//            WHERE f.deleted = FALSE
//              AND f.currentStatus = 'CLOSED'
//              AND (:fromDate IS NULL OR f.closedDate >= :fromDate)
//              AND (:toDate IS NULL OR f.closedDate <= :toDate)
//              AND (:sectionId IS NULL OR f.currentSectionId = :sectionId)
//              AND (:ownerId IS NULL OR f.currentOwnerId = :ownerId)
//              AND (:categoryId IS NULL OR f.categoryId = :categoryId)
//              AND (:priorityId IS NULL OR f.priorityId = :priorityId)
//            ORDER BY f.closedDate DESC
//            """)
//    java.util.List<FileEntity> disposedFiles(@Param("fromDate") java.time.LocalDate fromDate,
//                                             @Param("toDate") java.time.LocalDate toDate,
//                                             @Param("sectionId") UUID sectionId,
//                                             @Param("ownerId") UUID ownerId,
//                                             @Param("categoryId") UUID categoryId,
//                                             @Param("priorityId") UUID priorityId);
    
    @Query(value = """
    		SELECT *
    		FROM correspondence_files f
    		WHERE f.deleted = false
    		AND f.current_status = 'CLOSED'
    		AND (CAST(:fromDate AS DATE) IS NULL OR f.closed_date >= :fromDate)
    		AND (CAST(:toDate AS DATE) IS NULL OR f.closed_date <= :toDate)
    		AND (CAST(:sectionId AS UUID) IS NULL OR f.current_section_id = :sectionId)
    		AND (CAST(:ownerId AS UUID) IS NULL OR f.current_owner_id = :ownerId)
    		AND (CAST(:categoryId AS UUID) IS NULL OR f.category_id = :categoryId)
    		AND (CAST(:priorityId AS UUID) IS NULL OR f.priority_id = :priorityId)
    		ORDER BY f.closed_date DESC
    		""", nativeQuery = true)
    java.util.List<FileEntity> disposedFiles(@Param("fromDate") java.time.LocalDate fromDate,
                                             @Param("toDate") java.time.LocalDate toDate,
                                             @Param("sectionId") UUID sectionId,
                                             @Param("ownerId") UUID ownerId,
                                             @Param("categoryId") UUID categoryId,
                                             @Param("priorityId") UUID priorityId);

    // =================================================================
    // BCR-03 (Parts 3, 4, 11): ownership-scoped boxes and file registers.
    // Users never see all files by default — every box is scoped to the
    // caller. Additive queries only; existing queries are untouched.
    // =================================================================

    /** My Drafts: created by me and not yet marked to anyone. */
    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.createdBy = :userId
              AND f.currentStatus = 'DRAFT'
            """)
    Page<FileEntity> boxDrafts(@Param("userId") UUID userId, Pageable pageable);

    /** My Inbox: currently assigned to me and still live. */
    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentOwnerId = :userId
              AND f.currentStatus NOT IN ('DRAFT', 'CLOSED', 'ARCHIVED')
            """)
    Page<FileEntity> boxInbox(@Param("userId") UUID userId, Pageable pageable);

    /**
     * My Sent: files I marked onward and no longer hold. The set of file ids
     * I have moved is supplied by the workflow module through its facade, so
     * this module never queries workflow entities directly.
     */
    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.id IN :movedFileIds
              AND f.currentOwnerId <> :userId
            """)
    Page<FileEntity> boxSent(@Param("userId") UUID userId,
                             @Param("movedFileIds") java.util.List<UUID> movedFileIds,
                             Pageable pageable);

    /** My Returned: files returned to me for correction. */
    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentOwnerId = :userId
              AND f.currentStatus = 'RETURNED'
            """)
    Page<FileEntity> boxReturned(@Param("userId") UUID userId, Pageable pageable);

    /** My Closed: closed/archived files that I closed (ids from the facade). */
    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentStatus IN ('CLOSED', 'ARCHIVED')
              AND f.id IN :closedFileIds
            """)
    Page<FileEntity> boxClosed(@Param("closedFileIds") java.util.List<UUID> closedFileIds,
                               Pageable pageable);

    /**
     * File register (Part 11). sectionId NULL = central register (all sections).
     * Filters are all optional and null-tolerant.
     */
    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentStatus <> 'DRAFT'
              AND (:sectionId  IS NULL OR f.currentSectionId = :sectionId)
              AND (:ownerId    IS NULL OR f.currentOwnerId = :ownerId)
              AND (:priorityId IS NULL OR f.priorityId = :priorityId)
              AND (:status     IS NULL OR f.currentStatus = :status)
              AND (CAST(:fromDate AS date) IS NULL OR f.openedDate >= :fromDate)
              AND (CAST(:toDate   AS date) IS NULL OR f.openedDate <= :toDate)
            """)
    Page<FileEntity> register(@Param("sectionId") UUID sectionId,
                              @Param("ownerId") UUID ownerId,
                              @Param("priorityId") UUID priorityId,
                              @Param("status") String status,
                              @Param("fromDate") java.time.LocalDate fromDate,
                              @Param("toDate") java.time.LocalDate toDate,
                              Pageable pageable);

    /** BCR-03 Part 9: dispatch target picker — search files by number or subject. */
    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.deleted = FALSE
              AND f.currentStatus <> 'DRAFT'
              AND (LOWER(f.fileNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(f.subject) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY f.openedDate DESC
            """)
    java.util.List<FileEntity> searchForDispatchTarget(@Param("query") String query, Pageable pageable);
}
