package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.FilePriorityChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FilePriorityChangeRepository extends JpaRepository<FilePriorityChange, UUID> {

    List<FilePriorityChange> findByFileIdAndDeletedFalseOrderByChangedAtDesc(UUID fileId);
}
