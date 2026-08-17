package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsMetadataOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DmsMetadataOptionRepository extends JpaRepository<DmsMetadataOption, UUID> {

    List<DmsMetadataOption> findByMetadataFieldIdAndDeletedFalseOrderBySortOrderAscLabelAsc(
            UUID metadataFieldId);

    List<DmsMetadataOption> findByMetadataFieldIdInAndDeletedFalseOrderBySortOrderAscLabelAsc(
            Collection<UUID> metadataFieldIds);
}
