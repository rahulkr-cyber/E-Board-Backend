package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsMetadataField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DmsMetadataFieldRepository extends JpaRepository<DmsMetadataField, UUID> {

    Optional<DmsMetadataField> findByIdAndDeletedFalse(UUID id);

    Optional<DmsMetadataField> findByDocumentTypeIdAndFieldKeyIgnoreCaseAndActiveTrueAndDeletedFalse(
            UUID documentTypeId,
            String fieldKey);

    List<DmsMetadataField> findByDocumentTypeIdAndDeletedFalseOrderBySortOrderAscLabelAsc(
            UUID documentTypeId);

    List<DmsMetadataField> findByDocumentTypeIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscLabelAsc(
            UUID documentTypeId);

    List<DmsMetadataField> findByIdInAndDeletedFalse(Collection<UUID> ids);

    boolean existsByDocumentTypeIdAndFieldKeyIgnoreCaseAndDeletedFalse(
            UUID documentTypeId,
            String fieldKey);

    boolean existsByDocumentTypeIdAndFieldKeyIgnoreCaseAndIdNotAndDeletedFalse(
            UUID documentTypeId,
            String fieldKey,
            UUID id);

    boolean existsByDocumentTypeIdAndVisibilityConditionFieldKeyIgnoreCaseAndDeletedFalse(
            UUID documentTypeId,
            String fieldKey);

    boolean existsByDocumentTypeIdAndMandatoryConditionFieldKeyIgnoreCaseAndDeletedFalse(
            UUID documentTypeId,
            String fieldKey);

    boolean existsByParentFieldIdAndDeletedFalse(UUID parentFieldId);

    boolean existsByMasterSourceIdAndDeletedFalse(UUID masterSourceId);

    boolean existsByMasterSourceIdAndActiveTrueAndDeletedFalse(UUID masterSourceId);
}
