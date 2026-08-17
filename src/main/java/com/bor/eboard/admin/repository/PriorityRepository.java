package com.bor.eboard.admin.repository;

import com.bor.eboard.admin.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, UUID> {

    Optional<Priority> findByIdAndDeletedFalse(UUID id);

    boolean existsByCodeAndDeletedFalse(String code);

    List<Priority> findByDeletedFalseOrderBySortOrderAsc();
}
