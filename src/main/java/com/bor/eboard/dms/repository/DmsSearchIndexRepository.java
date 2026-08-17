package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsSearchIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DmsSearchIndexRepository extends JpaRepository<DmsSearchIndex, UUID> {

    Optional<DmsSearchIndex> findByDocumentIdAndDeletedFalse(UUID documentId);
}
