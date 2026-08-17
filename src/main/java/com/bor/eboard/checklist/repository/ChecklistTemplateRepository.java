package com.bor.eboard.checklist.repository;

import com.bor.eboard.checklist.entity.ChecklistTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, UUID> {
    Optional<ChecklistTemplate> findByIdAndDeletedFalse(UUID id);
    Optional<ChecklistTemplate> findByCodeAndDeletedFalse(String code);
    boolean existsByCodeAndDeletedFalse(String code);
    List<ChecklistTemplate> findByDeletedFalseOrderByNameAsc();
}
