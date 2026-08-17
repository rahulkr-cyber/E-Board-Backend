package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.Followup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowupRepository extends JpaRepository<Followup, UUID> {

    Optional<Followup> findByIdAndDeletedFalse(UUID id);

    List<Followup> findByFileIdAndDeletedFalseOrderByFollowupDateDesc(UUID fileId);

    List<Followup> findByLetterIdAndDeletedFalseOrderByFollowupDateDesc(UUID letterId);

    // BCR-03 Part 10: dispatch follow-ups.
    List<Followup> findByDispatchIdAndDeletedFalseOrderByFollowupDateAsc(UUID dispatchId);

    /** Open follow-ups whose reminder date has arrived — drives the scheduler. */
    @Query("""
            SELECT f FROM Followup f
            WHERE f.deleted = FALSE
              AND f.replyReceived = FALSE
              AND f.reminderDate IS NOT NULL
              AND f.reminderDate <= :today
            """)
    List<Followup> findDueReminders(@Param("today") java.time.LocalDate today);
}
