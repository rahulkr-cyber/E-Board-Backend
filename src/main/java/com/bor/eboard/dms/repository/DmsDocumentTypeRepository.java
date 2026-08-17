package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DmsDocumentTypeRepository extends JpaRepository<DmsDocumentType, UUID> {

    Optional<DmsDocumentType> findByIdAndDeletedFalse(UUID id);

    List<DmsDocumentType> findByDeletedFalseOrderBySortOrderAscNameAsc();

    List<DmsDocumentType> findByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc();

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedFalse(String name, UUID id);
}
