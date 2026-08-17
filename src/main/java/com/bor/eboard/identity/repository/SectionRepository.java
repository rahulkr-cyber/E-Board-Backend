package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {

    Optional<Section> findByIdAndDeletedFalse(UUID id);

    boolean existsByCodeAndDeletedFalse(String code);

    List<Section> findByDeletedFalseOrderByNameAsc();

    List<Section> findByDepartmentIdAndDeletedFalseOrderByNameAsc(UUID departmentId);
}
