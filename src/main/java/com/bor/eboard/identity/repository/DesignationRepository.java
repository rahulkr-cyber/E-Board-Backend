package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, UUID> {

    Optional<Designation> findByIdAndDeletedFalse(UUID id);

    boolean existsByCodeAndDeletedFalse(String code);

    List<Designation> findByDeletedFalseOrderByHierarchyLevelAsc();
}
