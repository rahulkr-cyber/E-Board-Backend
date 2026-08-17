package com.bor.eboard.admin.repository;

import com.bor.eboard.admin.entity.LetterCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LetterCategoryRepository extends JpaRepository<LetterCategory, UUID> {

    Optional<LetterCategory> findByIdAndDeletedFalse(UUID id);

    boolean existsByCodeAndDeletedFalse(String code);

    List<LetterCategory> findByDeletedFalseOrderByNameAsc();
}
