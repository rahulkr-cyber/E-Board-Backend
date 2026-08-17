package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    Optional<Attachment> findByIdAndDeletedFalse(UUID id);

    List<Attachment> findByLinkedEntityTypeAndLinkedEntityIdAndActiveTrueAndDeletedFalseOrderByUploadedAtAsc(
            String linkedEntityType, UUID linkedEntityId);

    List<Attachment> findByChecklistInstanceItemIdAndActiveTrueAndDeletedFalseOrderByUploadedAtAsc(
            UUID checklistInstanceItemId);

    long countByChecklistInstanceItemIdAndActiveTrueAndDeletedFalse(UUID checklistInstanceItemId);
}
