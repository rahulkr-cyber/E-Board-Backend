package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocumentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface DmsDocumentAuditRepository extends
        JpaRepository<DmsDocumentAudit, UUID>,
        JpaSpecificationExecutor<DmsDocumentAudit> {
}
