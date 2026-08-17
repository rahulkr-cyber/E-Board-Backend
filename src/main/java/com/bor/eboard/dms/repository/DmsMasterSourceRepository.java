package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.entity.DmsMasterSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DmsMasterSourceRepository extends JpaRepository<DmsMasterSource, UUID> {
    Optional<DmsMasterSource> findByIdAndDeletedFalse(UUID id);
    List<DmsMasterSource> findByDeletedFalseOrderByNameAsc();
    List<DmsMasterSource> findByActiveTrueAndDeletedFalseOrderByNameAsc();
    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndIdNotAndDeletedFalse(String name, UUID id);
}
