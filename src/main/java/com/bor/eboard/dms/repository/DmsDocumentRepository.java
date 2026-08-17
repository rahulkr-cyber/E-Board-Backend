package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DmsDocumentRepository extends JpaRepository<DmsDocument, UUID> {

    Optional<DmsDocument> findByIdAndDeletedFalse(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DmsDocument d WHERE d.id = :id AND d.deleted = FALSE")
    Optional<DmsDocument> findByIdForUpdate(@Param("id") UUID id);
}
