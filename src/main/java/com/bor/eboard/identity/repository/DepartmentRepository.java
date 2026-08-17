package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByIdAndDeletedFalse(UUID id);

    boolean existsByCodeAndDeletedFalse(String code);

    List<Department> findByDeletedFalseOrderByNameAsc();
}
