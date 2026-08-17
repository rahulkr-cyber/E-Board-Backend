package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocumentTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DmsDocumentTagRepository extends JpaRepository<DmsDocumentTag, UUID> {

    List<DmsDocumentTag> findByDocumentIdAndDeletedFalseOrderByTagValueAsc(UUID documentId);
}
