package com.bor.eboard.correspondence.facade.impl;

import com.bor.eboard.correspondence.entity.Letter;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.correspondence.repository.LetterRepository;
import com.bor.eboard.workflow.event.LetterCreatedEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
@Component
@RequiredArgsConstructor
public class CorrespondenceFacadeImpl implements CorrespondenceFacade {

    /** Initial status of an inward letter that just arrived in a section. */
    private static final String LETTER_STATUS_RECEIVED = "OPEN";
    
    private final com.bor.eboard.filestorage.service.AttachmentStorageService attachmentStorageService;
    private final com.bor.eboard.checklist.service.ChecklistService checklistService;
    /** File statuses that are read-only (08_BUSINESS_RULES.md file rules 8-9). */
    private static final java.util.Set<String> LOCKED_FILE_STATUSES =
            java.util.Set.of("CLOSED", "ARCHIVED");

    private static final java.util.Set<String> LOCKED_LETTER_STATUSES =
            java.util.Set.of("CLOSED", "ARCHIVED");
    private final ApplicationEventPublisher eventPublisher;
    private final LetterRepository letterRepository;
    private final com.bor.eboard.correspondence.repository.FileRepository fileRepository;
    private final com.bor.eboard.correspondence.repository.DispatchRepository dispatchRepository;
    private final com.bor.eboard.correspondence.repository.LetterMovementRepository letterMovementRepository;
    private final com.bor.eboard.correspondence.repository.FollowupRepository followupRepository;
    private final com.bor.eboard.admin.facade.MasterDataFacade masterDataFacade;
    private final com.bor.eboard.identity.facade.IdentityFacade identityFacade;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID createInwardLetterFromDiary(InwardLetterCommand command) {
        Letter letter = new Letter();
        letter.setDiaryEntryId(command.diaryEntryId());
        letter.setLetterDirection("INWARD");
        letter.setLetterType("ORIGINAL");
        letter.setLetterNumber(command.originalLetterNumber());
        letter.setReferenceNumber(command.referenceNumber());
        letter.setLetterDate(command.letterDate());
        letter.setSubject(command.subject());
        letter.setBody(command.description());
        letter.setSenderName(command.senderName());
        letter.setSenderDesignation(command.senderDesignation());
        letter.setSenderDepartment(command.senderDepartment());
        letter.setSenderAddress(command.senderAddress());
        letter.setReceiverDepartmentId(command.receiverDepartmentId());
        letter.setReceiverSectionId(command.receiverSectionId());
        letter.setReceiverUserId(command.receiverUserId());
        letter.setCategoryId(command.categoryId());
        letter.setPriorityId(command.priorityId());
        letter.setLanguageId(command.languageId());
        letter.setConfidential(command.confidential());
        letter.setDueDate(command.dueDate());
        letter.setReminderDate(command.reminderDate());
        letter.setCurrentOwnerId(command.receiverUserId());
        letter.setCurrentSectionId(command.receiverSectionId());
        letter.setCurrentStatus(LETTER_STATUS_RECEIVED);
//        return letterRepository.save(letter).getId();
        Letter saved = letterRepository.save(letter);

     // 🔥 Attachments linking
        attachmentStorageService.linkDiaryAttachmentsToLetter(command.diaryEntryId(), saved.getId());
        checklistService.linkDiaryToLetter(command.diaryEntryId(), saved.getId());

     // 🔥 Workflow trigger
     eventPublisher.publishEvent(
         new LetterCreatedEvent(
             saved.getId(),
             command.subject()
         )
     );

     return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<DiaryLetterState> findDiaryLetterState(UUID diaryEntryId) {
        return letterRepository.findByDiaryEntryIdAndDeletedFalse(diaryEntryId).stream()
                .findFirst()
                .map(letter -> new DiaryLetterState(
                        letter.getId(),
                        letter.getCurrentOwnerId(),
                        letter.getCurrentSectionId(),
                        letter.getCurrentStatus(),
                        letter.getDueDate(),
                        letter.getUpdatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DiaryLetterMovement> letterMovements(UUID letterId) {
        return letterMovementRepository.findByLetterIdAndDeletedFalseOrderByActionAtAsc(letterId).stream()
                .map(m -> new DiaryLetterMovement(m.getId(), m.getAction(), m.getFromUserId(),
                        m.getToUserId(), m.getFromSectionId(), m.getToSectionId(),
                        m.getRemarks(), m.getActionAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DiaryLetterFollowup> letterFollowups(UUID letterId) {
        return followupRepository.findByLetterIdAndDeletedFalseOrderByFollowupDateDesc(letterId).stream()
                .map(x -> new DiaryLetterFollowup(x.getId(), x.getFollowupDate(), x.getFollowupType(),
                        x.getDueDate(), x.getReminderDate(), x.getNextFollowupDate(),
                        x.getReplyReceived(), x.getReplyReceivedDate(), x.getStatus(),
                        x.getRemarks(), x.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DiaryLetterMetrics diaryLetterMetrics(UUID ownerId, UUID sectionId) {
        return new DiaryLetterMetrics(
                letterRepository.countDiaryLettersByScope(ownerId, sectionId, false),
                letterRepository.countDiaryLettersByScope(ownerId, sectionId, true));
    }

    @Override
    @Transactional(readOnly = true)
    public DiaryLetterMetrics diaryLetterMetricsBySections(
            java.util.Collection<UUID> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return new DiaryLetterMetrics(0, 0);
        }
        return new DiaryLetterMetrics(
                letterRepository.countDiaryLettersBySections(sectionIds, false),
                letterRepository.countDiaryLettersBySections(sectionIds, true));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Optional<LetterState> findLetterState(UUID letterId) {
        return letterRepository.findByIdAndDeletedFalse(letterId)
                .map(letter -> new LetterState(
                        letter.getId(),
                        letter.getSubject(),
                        letter.getCategoryId(),
                        letter.getPriorityId(),
                        letter.getReceiverDepartmentId(),
                        letter.getReceiverSectionId(),
                        letter.getCurrentOwnerId(),
                        letter.getCurrentSectionId(),
                        letter.getCurrentStatus(),
                        LOCKED_LETTER_STATUSES.contains(letter.getCurrentStatus()),
                        letter.getDiaryEntryId(),
                        letter.getLetterNumber()));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<UUID, LetterState> findLetterStates(java.util.Collection<UUID> letterIds) {
        if (letterIds == null || letterIds.isEmpty()) {
            return java.util.Map.of();
        }
        return letterRepository.findAllById(letterIds).stream()
                .filter(letter -> !Boolean.TRUE.equals(letter.getDeleted()))
                .collect(java.util.stream.Collectors.toMap(Letter::getId, letter -> new LetterState(
                        letter.getId(), letter.getSubject(), letter.getCategoryId(),
                        letter.getPriorityId(), letter.getReceiverDepartmentId(),
                        letter.getReceiverSectionId(), letter.getCurrentOwnerId(),
                        letter.getCurrentSectionId(), letter.getCurrentStatus(),
                        LOCKED_LETTER_STATUSES.contains(letter.getCurrentStatus()),
                        letter.getDiaryEntryId(), letter.getLetterNumber())));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyLetterMovement(UUID letterId, UUID newOwnerId, UUID newSectionId,
                                    String newStatus) {
        Letter letter = letterRepository.findByIdAndDeletedFalse(letterId)
                .orElseThrow(() -> new com.bor.eboard.common.exception
                        .ResourceNotFoundException("Letter", letterId));
        if (LOCKED_LETTER_STATUSES.contains(letter.getCurrentStatus())) {
            throw new com.bor.eboard.common.exception.BusinessException(
                    "Letter is " + letter.getCurrentStatus() + " and cannot be moved");
        }
        if (newOwnerId != null) {
            letter.setCurrentOwnerId(newOwnerId);
        }
        if (newSectionId != null) {
            letter.setCurrentSectionId(newSectionId);
        }
        if (newStatus != null) {
            letter.setCurrentStatus(newStatus);
        }
        letterRepository.save(letter);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Optional<FileState> findFileState(UUID fileId) {
        return fileRepository.findByIdAndDeletedFalse(fileId)
                .map(file -> new FileState(
                        file.getId(),
                        file.getFileNumber(),
                        file.getSubject(),
                        file.getCategoryId(),
                        file.getPriorityId(),
                        null,
                        file.getDepartmentId(),
                        file.getSectionId(),
                        file.getCurrentOwnerId(),
                        file.getCurrentSectionId(),
                        file.getCurrentStatus(),
                        LOCKED_FILE_STATUSES.contains(file.getCurrentStatus())));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<UUID, FileState> findFileStates(java.util.Collection<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<UUID, String> priorities = masterDataFacade.priorityNames();
        return fileRepository.findAllById(fileIds).stream()
                .filter(file -> !Boolean.TRUE.equals(file.getDeleted()))
                .collect(java.util.stream.Collectors.toMap(
                        com.bor.eboard.correspondence.entity.FileEntity::getId, file -> new FileState(
                                file.getId(), file.getFileNumber(), file.getSubject(),
                                file.getCategoryId(), file.getPriorityId(),
                                priorities.get(file.getPriorityId()),
                                file.getDepartmentId(), file.getSectionId(), file.getCurrentOwnerId(),
                                file.getCurrentSectionId(), file.getCurrentStatus(),
                                LOCKED_FILE_STATUSES.contains(file.getCurrentStatus()))));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<UUID, FileLetterReference> primaryLetterReferences(
            java.util.Collection<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<UUID, FileLetterReference> result = new java.util.LinkedHashMap<>();
        for (Letter letter : letterRepository
                .findByFileIdInAndDeletedFalseOrderByCreatedAtAsc(fileIds)) {
            if (letter.getFileId() != null) {
                result.putIfAbsent(letter.getFileId(), new FileLetterReference(
                        letter.getFileId(), letter.getId(), letter.getDiaryEntryId(),
                        letter.getLetterNumber()));
            }
        }
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyFileMovement(UUID fileId, UUID newOwnerId, UUID newSectionId,
                                  String newStatus) {
        com.bor.eboard.correspondence.entity.FileEntity file =
                fileRepository.findByIdAndDeletedFalse(fileId)
                        .orElseThrow(() -> new com.bor.eboard.common.exception
                                .ResourceNotFoundException("File", fileId));
        if (LOCKED_FILE_STATUSES.contains(file.getCurrentStatus())) {
            throw new com.bor.eboard.common.exception.BusinessException(
                    "File is " + file.getCurrentStatus() + " and cannot be moved");
        }
        if (newOwnerId != null) {
            file.setCurrentOwnerId(newOwnerId);
        }
        if (newSectionId != null) {
            file.setCurrentSectionId(newSectionId);
        }
        if (newStatus != null) {
            file.setCurrentStatus(newStatus);
        }
        fileRepository.save(file);
    }

    @Override
    @Transactional(readOnly = true)
    public FileMetrics fileMetrics(UUID ownerId, UUID sectionId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate monthStart = today.withDayOfMonth(1);
        return new FileMetrics(
                fileRepository.countByScopeAndStatus(ownerId, sectionId, "OPEN"),
                fileRepository.countByScopeAndStatus(ownerId, sectionId, "IN_PROGRESS"),
                fileRepository.countByScopeAndStatus(ownerId, sectionId, "PENDING_APPROVAL"),
                fileRepository.countByScopeAndStatus(ownerId, sectionId, "RETURNED"),
                fileRepository.countByScopeAndStatus(ownerId, sectionId, "REJECTED"),
                fileRepository.countByScopeAndStatus(ownerId, sectionId, "CLOSED"),
                fileRepository.countOpenedOn(ownerId, sectionId, today),
                fileRepository.countDisposedSince(ownerId, sectionId, monthStart));
    }

    @Override
    @Transactional(readOnly = true)
    public FileMetrics fileMetricsBySections(java.util.Collection<UUID> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return new FileMetrics(0, 0, 0, 0, 0, 0, 0, 0);
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate monthStart = today.withDayOfMonth(1);
        return new FileMetrics(
                fileRepository.countBySectionIdsAndStatus(sectionIds, "OPEN"),
                fileRepository.countBySectionIdsAndStatus(sectionIds, "IN_PROGRESS"),
                fileRepository.countBySectionIdsAndStatus(sectionIds, "PENDING_APPROVAL"),
                fileRepository.countBySectionIdsAndStatus(sectionIds, "RETURNED"),
                fileRepository.countBySectionIdsAndStatus(sectionIds, "REJECTED"),
                fileRepository.countBySectionIdsAndStatus(sectionIds, "CLOSED"),
                fileRepository.countOpenedOnBySectionIds(sectionIds, today),
                fileRepository.countDisposedSinceBySectionIds(sectionIds, monthStart));
    }

    @Override
    @Transactional(readOnly = true)
    public FileActivityMetrics fileActivity(UUID ownerId, UUID sectionId,
                                            java.time.LocalDate fromDate,
                                            java.time.LocalDate toDate) {
        return new FileActivityMetrics(
                fileRepository.countOpenedBetween(ownerId, sectionId, fromDate, toDate),
                fileRepository.countDisposedBetween(ownerId, sectionId, fromDate, toDate));
    }

    @Override
    @Transactional(readOnly = true)
    public FileActivityMetrics fileActivityBySections(
            java.util.Collection<UUID> sectionIds,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return new FileActivityMetrics(0, 0);
        }
        return new FileActivityMetrics(
                fileRepository.countOpenedBetweenBySectionIds(
                        sectionIds, fromDate, toDate),
                fileRepository.countDisposedBetweenBySectionIds(
                        sectionIds, fromDate, toDate));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<RecentFile> recentFiles(UUID ownerId, UUID sectionId, int limit) {
        return recentFiles(ownerId, sectionId, null, null, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<RecentFile> recentFiles(UUID ownerId, UUID sectionId,
                                                  java.time.LocalDate fromDate,
                                                  java.time.LocalDate toDate,
                                                  int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        java.util.List<com.bor.eboard.correspondence.entity.FileEntity> files =
                fromDate == null && toDate == null
                        ? fileRepository.recentByScope(ownerId, sectionId,
                                org.springframework.data.domain.PageRequest.of(0, safeLimit))
                        : fileRepository.recentByScopeAndDateRange(ownerId, sectionId,
                                fromDate, toDate,
                                org.springframework.data.domain.PageRequest.of(0, safeLimit));
        return files.stream()
                .map(file -> new RecentFile(
                        file.getId(), file.getFileNumber(), file.getSubject(),
                        file.getCurrentStatus(), file.getOpenedDate()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<RecentFile> recentFilesBySections(
            java.util.Collection<UUID> sectionIds,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate,
            int limit) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return java.util.List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return fileRepository.recentBySectionIdsAndDateRange(
                        sectionIds, fromDate, toDate,
                        org.springframework.data.domain.PageRequest.of(0, safeLimit)).stream()
                .map(file -> new RecentFile(
                        file.getId(), file.getFileNumber(), file.getSubject(),
                        file.getCurrentStatus(), file.getOpenedDate()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<FileReportRow> pendingReport(UUID sectionId, UUID officerId,
                                                       UUID categoryId, UUID priorityId) {
        java.util.Map<UUID, String> categories = masterDataFacade.categoryNames();
        java.util.Map<UUID, String> priorities = masterDataFacade.priorityNames();
        java.util.Map<UUID, String> sections = identityFacade.sectionNames();
        java.util.Map<UUID, String> users = identityFacade.userNames();
        java.time.LocalDate today = java.time.LocalDate.now();
        return fileRepository.pendingFiles(sectionId, officerId, categoryId, priorityId).stream()
                .map(file -> {
                    Long daysPending = file.getOpenedDate() != null
                            ? java.time.temporal.ChronoUnit.DAYS.between(file.getOpenedDate(), today)
                            : null;
                    return new FileReportRow(
                            file.getFileNumber(), file.getSubject(),
                            categories.get(file.getCategoryId()),
                            priorities.get(file.getPriorityId()),
                            users.get(file.getCurrentOwnerId()),
                            sections.get(file.getCurrentSectionId()),
                            file.getCurrentStatus(),
                            file.getOpenedDate(), file.getClosedDate(),
                            null, daysPending, null);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<FileReportRow> disposedReport(java.time.LocalDate fromDate,
                                                        java.time.LocalDate toDate,
                                                        UUID sectionId, UUID officerId,
                                                        UUID categoryId, UUID priorityId) {
        java.util.Map<UUID, String> categories = masterDataFacade.categoryNames();
        java.util.Map<UUID, String> priorities = masterDataFacade.priorityNames();
        java.util.Map<UUID, String> sections = identityFacade.sectionNames();
        java.util.Map<UUID, String> users = identityFacade.userNames();
        return fileRepository.disposedFiles(fromDate, toDate, sectionId, officerId,
                        categoryId, priorityId).stream()
                .map(file -> {
                    Long processingDays = (file.getOpenedDate() != null && file.getClosedDate() != null)
                            ? java.time.temporal.ChronoUnit.DAYS.between(
                                    file.getOpenedDate(), file.getClosedDate())
                            : null;
                    return new FileReportRow(
                            file.getFileNumber(), file.getSubject(),
                            categories.get(file.getCategoryId()),
                            priorities.get(file.getPriorityId()),
                            users.get(file.getCurrentOwnerId()),
                            sections.get(file.getCurrentSectionId()),
                            file.getCurrentStatus(),
                            file.getOpenedDate(), file.getClosedDate(),
                            null, null, processingDays);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DispatchReportRow> dispatchReport(java.time.LocalDate fromDate,
                                                            java.time.LocalDate toDate,
                                                            String recipient, String mode,
                                                            String status) {
        org.springframework.data.domain.Pageable all =
                org.springframework.data.domain.PageRequest.of(0, 5000);
        return dispatchRepository.search(null, blankToNull(recipient), blankToNull(mode),
                        blankToNull(status), fromDate, toDate, all).getContent().stream()
                .map(dispatch -> {
                    Letter letter = letterRepository.findByIdAndDeletedFalse(dispatch.getLetterId())
                            .orElse(null);
                    String fileNumber = letter != null && letter.getFileId() != null
                            ? fileRepository.findByIdAndDeletedFalse(letter.getFileId())
                                    .map(com.bor.eboard.correspondence.entity.FileEntity::getFileNumber)
                                    .orElse(null)
                            : null;
                    return new DispatchReportRow(
                            dispatch.getDispatchNumber(), dispatch.getDispatchDate(),
                            letter != null ? letter.getLetterNumber() : null,
                            fileNumber,
                            dispatch.getRecipientName(), dispatch.getRecipientDepartment(),
                            dispatch.getDispatchMode(), dispatch.getTrackingNumber(),
                            dispatch.getStatus());
                })
                .toList();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // =================================================================
    // BCR-03 Part 11: register rows.
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RegisterFileRow> registerRows(
            UUID sectionId, UUID ownerId, UUID priorityId, String status,
            java.time.LocalDate fromDate, java.time.LocalDate toDate,
            org.springframework.data.domain.Pageable pageable) {

        java.util.Map<UUID, String> users = identityFacade.userNames();
        java.util.Map<UUID, String> sections = identityFacade.sectionNames();
        java.util.Map<UUID, String> priorities = masterDataFacade.priorityNames();

        String normalisedStatus = (status == null || status.isBlank()) ? null : status;

        return fileRepository.register(sectionId, ownerId, priorityId, normalisedStatus,
                        fromDate, toDate, pageable)
                .map(f -> new RegisterFileRow(
                        f.getId(),
                        f.getFileNumber(),
                        f.getSubject(),
                        f.getCurrentOwnerId() == null ? null : users.get(f.getCurrentOwnerId()),
                        f.getCurrentSectionId() == null ? null : sections.get(f.getCurrentSectionId()),
                        f.getPriorityId() == null ? null : priorities.get(f.getPriorityId()),
                        f.getCurrentStatus(),
                        f.getOpenedDate(),
                        f.getClosedDate()));
    }
}
