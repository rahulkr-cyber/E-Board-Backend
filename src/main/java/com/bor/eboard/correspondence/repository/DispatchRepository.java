package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.DispatchRegisterEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DispatchRepository extends JpaRepository<DispatchRegisterEntry, UUID> {

    Optional<DispatchRegisterEntry> findByIdAndDeletedFalse(UUID id);

    List<DispatchRegisterEntry> findByLetterIdAndDeletedFalseOrderByDispatchDateAsc(UUID letterId);

    @Query("""
    	    SELECT d
    	    FROM DispatchRegisterEntry d
    	    WHERE d.deleted = FALSE
    	      AND (
    	            :dispatchNumber IS NULL
    	            OR LOWER(d.dispatchNumber) LIKE :dispatchNumber
    	      )
    	      AND (
    	            :recipient IS NULL
    	            OR LOWER(d.recipientName) LIKE :recipient
    	      )
    	      AND (
    	            :mode IS NULL
    	            OR d.dispatchMode = :mode
    	      )
    	      AND (
    	            :status IS NULL
    	            OR d.status = :status
    	      )
    	      AND d.dispatchDate >= :fromDate
    	      AND d.dispatchDate <= :toDate
    	    """)
    	Page<DispatchRegisterEntry> search(
    	        @Param("dispatchNumber") String dispatchNumber,
    	        @Param("recipient") String recipient,
    	        @Param("mode") String mode,
    	        @Param("status") String status,
    	        @Param("fromDate") LocalDate fromDate,
    	        @Param("toDate") LocalDate toDate,
    	        Pageable pageable);

    @Query("""
            SELECT d FROM DispatchRegisterEntry d
            WHERE d.deleted = FALSE
              AND d.dispatchDate >= :fromDate
              AND d.dispatchDate <= :toDate
            ORDER BY d.dispatchDate ASC, d.dispatchNumber ASC
            """)
    List<DispatchRegisterEntry> dispatchRegister(@Param("fromDate") LocalDate fromDate,
                                                 @Param("toDate") LocalDate toDate);
}
