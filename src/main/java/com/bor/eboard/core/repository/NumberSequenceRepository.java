package com.bor.eboard.core.repository;

import com.bor.eboard.core.entity.NumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NumberSequenceRepository extends JpaRepository<NumberSequence, UUID> {

    /**
     * Pessimistic row lock serializes concurrent number generation for the
     * same key/year, guaranteeing gap-free unique sequences.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ns FROM NumberSequence ns
            WHERE ns.sequenceKey = :key AND ns.sequenceYear = :year
            """)
    Optional<NumberSequence> lockByKeyAndYear(@Param("key") String key,
                                              @Param("year") int year);
}
