package com.bor.eboard.correspondence.facade;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.bor.eboard.correspondence.entity.Letter;

/**
 * Cross-module entry point into the Correspondence module
 * (02_ARCHITECTURE.md section 25: RegistryService -> CorrespondenceFacade;
 * other modules must never touch Correspondence repositories directly).
 */
public interface CorrespondenceFacade {

    /**
     * Command object describing the inward letter to create when Registry
     * forwards a diary entry to a section.
     */
    record InwardLetterCommand(
            UUID diaryEntryId,
            String originalLetterNumber,
            String referenceNumber,
            LocalDate letterDate,
            String subject,
            String description,
            String senderName,
            String senderDesignation,
            String senderDepartment,
            String senderAddress,
            UUID receiverDepartmentId,
            UUID receiverSectionId,
            UUID receiverUserId,
            UUID categoryId,
            UUID priorityId,
            UUID languageId,
            boolean confidential,
            LocalDate dueDate,
            LocalDate reminderDate) {
    }

    /**
     * Creates the INWARD/ORIGINAL letter record for a forwarded diary entry
     * and returns its id. Participates in the caller's transaction
     * (forward diary + create letter is one atomic unit,
     * 02_ARCHITECTURE.md section 26).
     */
    UUID createInwardLetterFromDiary(InwardLetterCommand command);

    /**
     * Read-only live state of the Letter created from a Diary entry. Registry
     * uses this projection for tracking and never owns or copies workflow state.
     */
    record DiaryLetterState(
            UUID letterId,
            UUID currentOwnerId,
            UUID currentSectionId,
            String currentStatus,
            LocalDate dueDate,
            java.time.LocalDateTime updatedAt) {
    }

    java.util.Optional<DiaryLetterState> findDiaryLetterState(UUID diaryEntryId);

    record DiaryLetterMovement(UUID id, String action, UUID fromUserId, UUID toUserId,
                               UUID fromSectionId, UUID toSectionId, String remarks,
                               java.time.LocalDateTime actionAt) { }

    record DiaryLetterFollowup(UUID id, java.time.LocalDate followupDate, String followupType,
                               java.time.LocalDate dueDate, java.time.LocalDate reminderDate,
                               java.time.LocalDate nextFollowupDate, Boolean replyReceived,
                               java.time.LocalDate replyReceivedDate, String status, String remarks,
                               java.time.LocalDateTime createdAt) { }

    java.util.List<DiaryLetterMovement> letterMovements(UUID letterId);

    java.util.List<DiaryLetterFollowup> letterFollowups(UUID letterId);

    record DiaryLetterMetrics(long forwarded, long disposed) { }

    DiaryLetterMetrics diaryLetterMetrics(UUID ownerId, UUID sectionId);

    DiaryLetterMetrics diaryLetterMetricsBySections(
            java.util.Collection<UUID> sectionIds);

    /** Lightweight live state of a Letter for the shared Workflow engine. */
    record LetterState(
            UUID letterId,
            String subject,
            UUID categoryId,
            UUID priorityId,
            UUID departmentId,
            UUID sectionId,
            UUID currentOwnerId,
            UUID currentSectionId,
            String currentStatus,
            boolean closed,
            UUID diaryEntryId,
            String letterNumber) {
    }

    java.util.Optional<LetterState> findLetterState(UUID letterId);

    java.util.Map<UUID, LetterState> findLetterStates(java.util.Collection<UUID> letterIds);
   

    /** Applies a workflow-driven ownership/section/status change to a Letter. */
    void applyLetterMovement(UUID letterId, UUID newOwnerId, UUID newSectionId, String newStatus);

    /**
     * Lightweight snapshot of a file's routing state, exposed so the
     * Workflow module (Phase 5) can read/route without touching the
     * correspondence repositories directly (02_ARCHITECTURE.md section 25).
     */
    record FileState(
            UUID fileId,
            String fileNumber,
            String subject,
            UUID categoryId,
            UUID priorityId,
            String priorityName,
            UUID departmentId,
            UUID sectionId,
            UUID currentOwnerId,
            UUID currentSectionId,
            String currentStatus,
            boolean closed) {
    }

    java.util.Optional<FileState> findFileState(UUID fileId);

    java.util.Map<UUID, FileState> findFileStates(java.util.Collection<UUID> fileIds);

    record FileLetterReference(UUID fileId, UUID letterId, UUID diaryEntryId, String letterNumber) { }

    java.util.Map<UUID, FileLetterReference> primaryLetterReferences(
            java.util.Collection<UUID> fileIds);

    /**
     * Applies a workflow-driven ownership/section/status change to a file
     * within the caller's transaction. Used by the Workflow module for
     * forward/approve/return/reassign actions.
     */
    void applyFileMovement(UUID fileId, UUID newOwnerId, UUID newSectionId, String newStatus);

    /**
     * Aggregated file counts for a dashboard scope (null owner and section
     * mean global; otherwise scope to that owner and/or section). Exposed so
     * the Dashboard module composes metrics without reaching into
     * correspondence repositories (02_ARCHITECTURE.md section 25).
     */
    record FileMetrics(
            long open,
            long inProgress,
            long pendingApproval,
            long returned,
            long rejected,
            long closed,
            long openedToday,
            long disposedThisMonth) {
    }

    FileMetrics fileMetrics(UUID ownerId, UUID sectionId);

    FileMetrics fileMetricsBySections(java.util.Collection<UUID> sectionIds);

    /** Range-aware activity counts used by reusable dashboard filters. */
    record FileActivityMetrics(long opened, long disposed) {
    }

    FileActivityMetrics fileActivity(UUID ownerId, UUID sectionId,
                                     java.time.LocalDate fromDate,
                                     java.time.LocalDate toDate);

    FileActivityMetrics fileActivityBySections(
            java.util.Collection<UUID> sectionIds,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate);

    /** A lightweight recent-file row for dashboard lists. */
    record RecentFile(
            UUID fileId,
            String fileNumber,
            String subject,
            String currentStatus,
            java.time.LocalDate openedDate) {
    }

    java.util.List<RecentFile> recentFiles(UUID ownerId, UUID sectionId, int limit);

    java.util.List<RecentFile> recentFiles(UUID ownerId, UUID sectionId,
                                           java.time.LocalDate fromDate,
                                           java.time.LocalDate toDate,
                                           int limit);

    java.util.List<RecentFile> recentFilesBySections(
            java.util.Collection<UUID> sectionIds,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate,
            int limit);

    // ---- Report row providers (read-only, for the Reports module) ----

    /** A file row for the Pending / Disposed reports. */
    record FileReportRow(
            String fileNumber,
            String subject,
            String categoryName,
            String priorityName,
            String currentOwnerName,
            String currentSectionName,
            String status,
            java.time.LocalDate openedDate,
            java.time.LocalDate closedDate,
            java.time.LocalDate dueDate,
            Long daysPending,
            Long processingDays) {
    }

    java.util.List<FileReportRow> pendingReport(UUID sectionId, UUID officerId,
                                                UUID categoryId, UUID priorityId);

    java.util.List<FileReportRow> disposedReport(java.time.LocalDate fromDate,
                                                 java.time.LocalDate toDate,
                                                 UUID sectionId, UUID officerId,
                                                 UUID categoryId, UUID priorityId);

    /** A dispatch row for the Dispatch Register report. */
    record DispatchReportRow(
            String dispatchNumber,
            java.time.LocalDate dispatchDate,
            String letterNumber,
            String fileNumber,
            String recipientName,
            String recipientDepartment,
            String dispatchMode,
            String trackingNumber,
            String status) {
    }

    java.util.List<DispatchReportRow> dispatchReport(java.time.LocalDate fromDate,
                                                     java.time.LocalDate toDate,
                                                     String recipient, String mode,
                                                     String status);

    // =================================================================
    // BCR-03 Part 11: Section / Central file registers. The register module
    // reads files through this facade — it never touches correspondence
    // repositories directly.
    // =================================================================

    record RegisterFileRow(
            UUID fileId,
            String fileNumber,
            String subject,
            String currentHolder,
            String sectionName,
            String priorityName,
            String status,
            java.time.LocalDate openedDate,
            java.time.LocalDate closedDate) {
    }

    /**
     * Paged register rows. {@code sectionId} null = all sections (central).
     * Drafts are excluded — an unmarked file is private to its creator.
     */
    org.springframework.data.domain.Page<RegisterFileRow> registerRows(
            UUID sectionId, UUID ownerId, UUID priorityId, String status,
            java.time.LocalDate fromDate, java.time.LocalDate toDate,
            org.springframework.data.domain.Pageable pageable);
}
