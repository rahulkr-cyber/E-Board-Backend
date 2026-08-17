package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocumentShare;
import com.bor.eboard.dms.security.DmsPrincipalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DmsDocumentShareRepository extends JpaRepository<DmsDocumentShare, UUID> {

    List<DmsDocumentShare>
        findByDocumentIdAndActiveTrueAndDeletedFalseOrderBySharedAtDesc(UUID documentId);

    Optional<DmsDocumentShare>
        findByIdAndDocumentIdAndDeletedFalse(UUID id, UUID documentId);

    Optional<DmsDocumentShare>
        findFirstByDocumentIdAndPrincipalTypeAndPrincipalIdAndActiveTrueAndDeletedFalse(
                UUID documentId,
                DmsPrincipalType principalType,
                UUID principalId);
}
