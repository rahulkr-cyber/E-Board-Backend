package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsMasterSourceParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DmsMasterSourceParameterRepository extends JpaRepository<DmsMasterSourceParameter, UUID> {
    List<DmsMasterSourceParameter> findByMasterSourceIdAndDeletedFalseOrderBySortOrderAscParameterNameAsc(UUID masterSourceId);
}
