package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DmsDocumentHistoryRepository extends JpaRepository<DmsDocumentHistory, UUID> {

    List<DmsDocumentHistory> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
