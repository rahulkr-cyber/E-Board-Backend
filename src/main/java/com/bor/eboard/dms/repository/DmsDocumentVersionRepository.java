package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DmsDocumentVersionRepository extends JpaRepository<DmsDocumentVersion, UUID> {

    List<DmsDocumentVersion> findByDocumentIdAndDeletedFalseOrderByVersionNumberDesc(UUID documentId);

    Optional<DmsDocumentVersion> findByDocumentIdAndVersionNumberAndDeletedFalse(
            UUID documentId,
            Integer versionNumber);

    Optional<DmsDocumentVersion> findFirstByDocumentIdAndDeletedFalseOrderByVersionNumberDesc(
            UUID documentId);
}
