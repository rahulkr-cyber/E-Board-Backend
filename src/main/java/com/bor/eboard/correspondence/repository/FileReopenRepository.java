package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.FileReopen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileReopenRepository extends JpaRepository<FileReopen, UUID> {

    List<FileReopen> findByFileIdAndDeletedFalseOrderByReopenedAtDesc(UUID fileId);
}
