package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DmsDocumentMetadataRepository extends JpaRepository<DmsDocumentMetadata, UUID> {

    List<DmsDocumentMetadata> findByDocumentIdAndDeletedFalseOrderByFieldLabelAsc(UUID documentId);
}
