package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocumentPermission;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.security.DmsPrincipalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DmsDocumentPermissionRepository
        extends JpaRepository<DmsDocumentPermission, UUID> {

    List<DmsDocumentPermission>
        findByDocumentIdAndActiveTrueAndDeletedFalseOrderByCreatedAtAsc(UUID documentId);

    Optional<DmsDocumentPermission>
        findByIdAndDocumentIdAndDeletedFalse(UUID id, UUID documentId);

    Optional<DmsDocumentPermission>
        findFirstByDocumentIdAndPrincipalTypeAndPrincipalIdAndAccessLevelAndShareIdIsNullAndDeletedFalseOrderByCreatedAtDesc(
                UUID documentId,
                DmsPrincipalType principalType,
                UUID principalId,
                DmsDocumentAccessLevel accessLevel);

    List<DmsDocumentPermission>
        findByShareIdAndDeletedFalse(UUID shareId);

    List<DmsDocumentPermission>
        findByDocumentIdAndPrincipalTypeAndPrincipalIdAndActiveTrueAndDeletedFalse(
                UUID documentId,
                DmsPrincipalType principalType,
                UUID principalId);
}
