package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.LetterMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LetterMovementRepository extends JpaRepository<LetterMovement, UUID> {

    List<LetterMovement> findByLetterIdAndDeletedFalseOrderByActionAtAsc(UUID letterId);

    /** Letter ids this user has marked onward (My Sent Letters). */
    @Query("""
            SELECT DISTINCT m.letterId FROM LetterMovement m
            WHERE m.fromUserId = :userId
            """)
    List<UUID> findLetterIdsMovedBy(@Param("userId") UUID userId);
}
