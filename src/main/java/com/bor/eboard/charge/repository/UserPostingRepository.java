package com.bor.eboard.charge.repository;

import com.bor.eboard.charge.entity.UserPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPostingRepository extends JpaRepository<UserPosting, UUID> {

    List<UserPosting> findByUserIdAndDeletedFalseOrderByPostingStartDateDesc(UUID userId);

    List<UserPosting> findByUserIdAndActiveTrueAndDeletedFalseOrderByPostingStartDateDesc(UUID userId);

    Optional<UserPosting> findFirstByUserIdAndActiveTrueAndDeletedFalseOrderByPostingStartDateDesc(UUID userId);

    List<UserPosting> findByUserIdAndDepartmentIdAndSectionIdAndDesignationIdAndDeletedFalseOrderByPostingStartDateDesc(
            UUID userId, UUID departmentId, UUID sectionId, UUID designationId);

    Optional<UserPosting> findByIdAndDeletedFalse(UUID id);

    Optional<UserPosting> findBySourceEntityTypeAndSourceEntityIdAndDeletedFalse(
            String sourceEntityType, UUID sourceEntityId);

    List<UserPosting> findByDeletedFalse();
}
