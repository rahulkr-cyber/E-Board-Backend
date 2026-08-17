package com.bor.eboard.correspondence.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.core.service.NumberGenerationService;
import com.bor.eboard.correspondence.dto.AttachmentInfo;
import com.bor.eboard.correspondence.dto.CloseFileRequest;
import com.bor.eboard.correspondence.dto.ChangePriorityRequest;
import com.bor.eboard.correspondence.dto.CreateFileRequest;
import com.bor.eboard.correspondence.dto.MarkFileRequest;
import com.bor.eboard.correspondence.dto.PriorityChangeResponse;
import com.bor.eboard.correspondence.dto.ReopenFileRequest;
import com.bor.eboard.correspondence.dto.ReopenHistoryResponse;
import com.bor.eboard.correspondence.entity.FilePriorityChange;
import com.bor.eboard.correspondence.entity.FileReopen;
import com.bor.eboard.correspondence.repository.FilePriorityChangeRepository;
import com.bor.eboard.correspondence.repository.FileReopenRepository;
import com.bor.eboard.correspondence.dto.FileResponse;
import com.bor.eboard.correspondence.dto.LetterResponse;
import com.bor.eboard.correspondence.dto.TimelineEntry;
import com.bor.eboard.correspondence.entity.DispatchRegisterEntry;
import com.bor.eboard.correspondence.entity.FileEntity;
import com.bor.eboard.correspondence.entity.Followup;
import com.bor.eboard.correspondence.entity.Letter;
import com.bor.eboard.correspondence.entity.Reminder;
import com.bor.eboard.correspondence.mapper.CorrespondenceLookups;
import com.bor.eboard.correspondence.mapper.CorrespondenceMapper;
import com.bor.eboard.correspondence.mapper.LookupProvider;
import com.bor.eboard.correspondence.repository.DispatchRepository;
import com.bor.eboard.correspondence.repository.FileRepository;
import com.bor.eboard.correspondence.repository.FollowupRepository;
import com.bor.eboard.correspondence.repository.LetterRepository;
import com.bor.eboard.correspondence.repository.ReminderRepository;
import com.bor.eboard.correspondence.service.FileService;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Locale;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * File implementation.
 *
 * Enforced rules (06_BUSINESS_RULES.md sections 4-5):
 * - File number BOR/FILE/{YEAR}/{SEQ}: system-generated, immutable.
 * - New file opens OPEN, owned by the creator in the chosen section.
 * - Closed/archived files are read-only (no letters, no attachments, no re-close).
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String MODULE = "CORRESPONDENCE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final java.util.Set<String> LOCKED = java.util.Set.of("CLOSED", "ARCHIVED");

    private final FileRepository fileRepository;
    private final LetterRepository letterRepository;
    private final FollowupRepository followupRepository;
    private final ReminderRepository reminderRepository;
    private final DispatchRepository dispatchRepository;
    private final NumberGenerationService numberGenerationService;
    private final AttachmentStorageService attachmentStorageService;
    private final MasterDataFacade masterDataFacade;
    private final IdentityFacade identityFacade;
    private final CorrespondenceMapper mapper;
    private final LookupProvider lookupProvider;
    private final AuditService auditService;
    // BCR-03 additions
    private final FilePriorityChangeRepository priorityChangeRepository;
    private final FileReopenRepository reopenRepository;
    private final com.bor.eboard.assignment.facade.AssignmentFacade assignmentFacade;
    private final com.bor.eboard.workflow.facade.WorkflowEscalationFacade workflowFacade;
    private final com.bor.eboard.workflow.engine.WorkflowMovementService movementService;
    private final com.bor.eboard.notification.facade.NotificationFacade notificationFacade;

    @Override
    @Transactional
    public FileResponse create(CreateFileRequest request) {
        validateReferences(request.getCategoryId(), request.getPriorityId(),
                request.getDepartmentId(), request.getSectionId());

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        NumberGenerationService.GeneratedNumber fileNumber =
                numberGenerationService.next("FILE", "BOR/FILE");

        FileEntity file = new FileEntity();
        file.setFileNumber(fileNumber.formatted());
        file.setFileYear(fileNumber.year());
        file.setFileSequence(fileNumber.sequence());
        file.setSubject(request.getSubject());
        file.setDescription(request.getDescription());
        file.setCategoryId(request.getCategoryId());
        file.setPriorityId(request.getPriorityId());
        file.setDepartmentId(request.getDepartmentId());
        file.setSectionId(request.getSectionId());
        file.setCurrentOwnerId(currentUserId);
        file.setCurrentSectionId(request.getSectionId());
        // BCR-03 Part 2/3: a new file starts as DRAFT and is visible only to its
        // creator. It becomes visible to anyone else only when explicitly marked.
        file.setCurrentStatus(STATUS_DRAFT);
        file.setOpenedDate(LocalDate.now());
        file.setConfidential(Boolean.TRUE.equals(request.getConfidential()));
        file = fileRepository.save(file);

        // If opened from a diary entry, tie the originating inward letter to this file.
        if (request.getDiaryEntryId() != null) {
            List<Letter> inwardLetters =
                    letterRepository.findByDiaryEntryIdAndDeletedFalse(request.getDiaryEntryId());
            for (Letter letter : inwardLetters) {
                if (letter.getFileId() == null) {
                    letter.setFileId(file.getId());
                    letterRepository.save(letter);
                }
            }
        }

        auditService.record(MODULE, "FILE", file.getId(), "CREATE", null,
                "fileNumber=" + file.getFileNumber());
        return assemble(file);
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getById(UUID id) {
        return assemble(findFile(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FileResponse> search(
            String fileNumber,
            String status,
            UUID sectionId,
            UUID ownerId,
            PaginationRequest pagination) {

        String normalizedFileNumber = StringUtils.hasText(fileNumber)
                ? "%" + fileNumber.trim().toLowerCase(Locale.ROOT) + "%"
                : null;

        String normalizedStatus = StringUtils.hasText(status)
                ? status.trim()
                : null;

        Page<FileEntity> page = fileRepository.search(
                normalizedFileNumber,
                normalizedStatus,
                sectionId,
                ownerId,
                pagination.toPageable());

        CorrespondenceLookups lookups = lookupProvider.load();

        List<FileResponse> content = page.getContent()
                .stream()
                .map(file -> mapper.toFileResponse(file, lookups, List.of(), List.of()))
                .toList();

        return PageResponse.of(content, page);
    }

    @Override
    @Transactional
    public FileResponse close(UUID id, CloseFileRequest request) {
        FileEntity file = findFile(id);
        if (LOCKED.contains(file.getCurrentStatus())) {
            throw new BusinessException(
                    "File is already " + file.getCurrentStatus());
        }
        String oldStatus = file.getCurrentStatus();
        file.setCurrentStatus(STATUS_CLOSED);
        file.setClosedDate(LocalDate.now());
        file = fileRepository.save(file);
        auditService.record(MODULE, "FILE", file.getId(), "CLOSE", oldStatus,
                STATUS_CLOSED + (request.getRemarks() != null
                        ? ", remarks=" + truncate(request.getRemarks()) : ""));
        return assemble(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineEntry> timeline(UUID id) {
        FileEntity file = findFile(id);
        CorrespondenceLookups lookups = lookupProvider.load();
        List<TimelineEntry> events = new ArrayList<>();

        events.add(TimelineEntry.builder()
                .timestamp(file.getCreatedAt())
                .eventType("FILE")
                .title("File opened")
                .detail(file.getFileNumber() + " — " + file.getSubject())
                .referenceId(file.getId())
                .actorName(lookups.user(file.getCreatedBy()))
                .build());

        for (Letter letter : letterRepository.findByFileIdAndDeletedFalseOrderByCreatedAtAsc(id)) {
            events.add(TimelineEntry.builder()
                    .timestamp(letter.getCreatedAt())
                    .eventType("LETTER")
                    .title(letter.getLetterDirection() + " " + letter.getLetterType() + " letter")
                    .detail(letter.getSubject())
                    .referenceId(letter.getId())
                    .actorName(lookups.user(letter.getCreatedBy()))
                    .build());
        }
        for (Followup followup : followupRepository.findByFileIdAndDeletedFalseOrderByFollowupDateDesc(id)) {
            events.add(TimelineEntry.builder()
                    .timestamp(followup.getCreatedAt())
                    .eventType("FOLLOWUP")
                    .title("Follow-up (" + followup.getStatus() + ")")
                    .detail(followup.getRemarks())
                    .referenceId(followup.getId())
                    .actorName(lookups.user(followup.getCreatedBy()))
                    .build());
        }
        for (Reminder reminder : reminderRepository.findByFileIdAndDeletedFalseOrderByReminderDateDesc(id)) {
            events.add(TimelineEntry.builder()
                    .timestamp(reminder.getCreatedAt())
                    .eventType("REMINDER")
                    .title("Reminder (" + reminder.getStatus() + ")")
                    .detail(reminder.getRemarks())
                    .referenceId(reminder.getId())
                    .actorName(lookups.user(reminder.getGeneratedBy()))
                    .build());
        }
        // Dispatches linked via this file's letters
        List<Letter> letters = letterRepository.findByFileIdAndDeletedFalseOrderByCreatedAtAsc(id);
        for (Letter letter : letters) {
            for (DispatchRegisterEntry dispatch :
                    dispatchRepository.findByLetterIdAndDeletedFalseOrderByDispatchDateAsc(letter.getId())) {
                events.add(TimelineEntry.builder()
                        .timestamp(dispatch.getCreatedAt())
                        .eventType("DISPATCH")
                        .title("Dispatch " + dispatch.getDispatchNumber()
                                + " (" + dispatch.getStatus() + ")")
                        .detail("To " + dispatch.getRecipientName())
                        .referenceId(dispatch.getId())
                        .actorName(lookups.user(dispatch.getCreatedBy()))
                        .build());
            }
        }

        events.sort(Comparator.comparing(TimelineEntry::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    @Override
    @Transactional
    public AttachmentInfo uploadAttachment(UUID fileId, UUID documentTypeId, MultipartFile file) {
        FileEntity entity = findFile(fileId);
        if (LOCKED.contains(entity.getCurrentStatus())) {
            throw new BusinessException(
                    "Attachments cannot be added to a " + entity.getCurrentStatus() + " file");
        }
        AttachmentStorageService.StoredAttachment stored =
                attachmentStorageService.store("FILE", entity.getId(), documentTypeId, file);
        return mapper.toAttachmentInfo(stored);
    }

    // ------------------------------------------------------------------

    private FileEntity findFile(UUID id) {
        return fileRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("File", id));
    }

    private FileResponse assemble(FileEntity file) {
        CorrespondenceLookups lookups = lookupProvider.load();
        List<LetterResponse> letters =
                letterRepository.findByFileIdAndDeletedFalseOrderByCreatedAtAsc(file.getId())
                        .stream()
                        .map(letter -> mapper.toLetterResponse(letter, file.getFileNumber(),
                                lookups, attachmentsFor("LETTER", letter.getId())))
                        .toList();
        return mapper.toFileResponse(file, lookups, letters,
                attachmentsFor("FILE", file.getId()));
    }

    private List<AttachmentInfo> attachmentsFor(String entityType, UUID entityId) {
        return attachmentStorageService.listFor(entityType, entityId).stream()
                .map(mapper::toAttachmentInfo).toList();
    }

    private void validateReferences(UUID categoryId, UUID priorityId,
                                    UUID departmentId, UUID sectionId) {
        if (!masterDataFacade.categoryExists(categoryId)) {
            throw new ValidationException("Invalid category reference");
        }
        if (!masterDataFacade.priorityExists(priorityId)) {
            throw new ValidationException("Invalid priority reference");
        }
        if (!identityFacade.departmentExists(departmentId)) {
            throw new ValidationException("Invalid department reference");
        }
        IdentityFacade.SectionRef section = identityFacade.findSection(sectionId)
                .orElseThrow(() -> new ValidationException("Invalid section reference"));
        if (!section.departmentId().equals(departmentId)) {
            throw new ValidationException(
                    "Section does not belong to the selected department");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 120 ? value.substring(0, 120) + "..." : value;
    }

    // =====================================================================
    // BCR-03 Part 4: file boxes. Users never see all files by default; every
    // box is scoped to the caller.
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FileResponse> box(String box, PaginationRequest pagination) {
        UUID me = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        Pageable pageable = pagination.toPageable();
        String key = box == null ? "" : box.trim().toLowerCase();

        Page<FileEntity> page = switch (key) {
            case "drafts" -> fileRepository.boxDrafts(me, pageable);
            case "inbox" -> fileRepository.boxInbox(me, pageable);
            case "returned" -> fileRepository.boxReturned(me, pageable);
            case "sent" -> {
                List<UUID> moved = workflowFacade.fileIdsMovedBy(me);
                yield moved.isEmpty()
                        ? Page.empty(pageable)
                        : fileRepository.boxSent(me, moved, pageable);
            }
            case "closed" -> {
                List<UUID> closed = workflowFacade.fileIdsClosedBy(me);
                yield closed.isEmpty()
                        ? Page.empty(pageable)
                        : fileRepository.boxClosed(closed, pageable);
            }
            default -> throw new ValidationException(
                    "Unknown box '" + box + "'. Use drafts, inbox, sent, returned or closed.");
        };

        CorrespondenceLookups lookups = lookupProvider.load();
        List<FileResponse> content = page.getContent().stream()
                .map(f -> mapper.toFileResponse(f, lookups, List.of(), List.of()))
                .toList();
        return PageResponse.of(content, page);
    }

    // =====================================================================
    // BCR-03 Part 6: Mark. Replaces a bare forward — the user explicitly picks
    // section, role and user; the assignment rules decide who is eligible.
    // =====================================================================

    @Override
    @Transactional
    public FileResponse mark(UUID id, MarkFileRequest request) {
        UUID me = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        FileEntity file = findFile(id);

        if (!me.equals(file.getCurrentOwnerId())) {
            throw new ForbiddenException("Only the current owner may mark this file.");
        }
        if (STATUS_CLOSED.equals(file.getCurrentStatus())) {
            throw new BusinessException("A closed file cannot be marked. Reopen it first.");
        }
        if (me.equals(request.getToUserId())) {
            throw new ValidationException("A file cannot be marked to yourself.");
        }

        IdentityFacade.UserRef target = identityFacade.findUser(request.getToUserId())
                .orElseThrow(() -> new ValidationException("Invalid target user"));
        if (!target.active()) {
            throw new ValidationException("The target user is not active.");
        }

        UUID toSection = request.getToSectionId() != null
                ? request.getToSectionId() : target.sectionId();

        // The assignment rules — not Java — decide whether this mark is allowed.
        List<UUID> myRoles = identityFacade.activeRoleIdsOf(me);
        if (!assignmentFacade.canMark(myRoles, file.getCurrentSectionId(),
                request.getToUserId(), toSection)) {
            throw new ForbiddenException(
                    "No assignment rule permits you to mark this file to that user.");
        }

        UUID fromUser = file.getCurrentOwnerId();
        UUID fromSection = file.getCurrentSectionId();
        String previousStatus = file.getCurrentStatus();

        file.setCurrentOwnerId(request.getToUserId());
        file.setCurrentSectionId(toSection);
        // A draft becomes live the moment it is first marked.
        file.setCurrentStatus(STATUS_IN_PROGRESS);
        fileRepository.save(file);

        movementService.record(file.getId(), null, null, fromUser, request.getToUserId(),
                fromSection, toSection, "MARK", request.getRemarks());

        auditService.record(MODULE, "FILE", file.getId(), "MARK",
                previousStatus, "Marked to " + target.fullName());

        notificationFacade.notify(request.getToUserId(),
                "File marked to you",
                "File " + file.getFileNumber() + " — " + file.getSubject(),
                "TASK_ASSIGNED", "FILE", file.getId(), "NORMAL");

        return mapper.toFileResponse(file, lookupProvider.load(), List.of(), List.of());
    }

    // =====================================================================
    // BCR-03 Part 7: priority change (Chairman / Commissioner & Secretary only —
    // enforced by FILE_PRIORITY_CHANGE at the controller).
    // =====================================================================

    @Override
    @Transactional
    public FileResponse changePriority(UUID id, ChangePriorityRequest request) {
        UUID me = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        FileEntity file = findFile(id);

        if (!masterDataFacade.priorityExists(request.getPriorityId())) {
            throw new ValidationException("Invalid priorityId");
        }
        UUID oldPriority = file.getPriorityId();
        if (request.getPriorityId().equals(oldPriority)) {
            throw new ValidationException("The file already has that priority.");
        }

        file.setPriorityId(request.getPriorityId());
        fileRepository.save(file);

        FilePriorityChange history = new FilePriorityChange();
        history.setFileId(file.getId());
        history.setOldPriorityId(oldPriority);
        history.setNewPriorityId(request.getPriorityId());
        history.setChangedBy(me);
        history.setChangedAt(LocalDateTime.now());
        history.setRemarks(request.getRemarks());
        priorityChangeRepository.save(history);

        Map<UUID, String> priorities = masterDataFacade.priorityNames();
        auditService.record(MODULE, "FILE", file.getId(), "PRIORITY_CHANGE",
                oldPriority == null ? null : priorities.get(oldPriority),
                priorities.get(request.getPriorityId())
                        + (request.getRemarks() == null ? "" : " — " + request.getRemarks()));

        return mapper.toFileResponse(file, lookupProvider.load(), List.of(), List.of());
    }

    // =====================================================================
    // BCR-03 Part 8: reopen a closed file (Chairman / Commissioner & Secretary).
    // History is never altered; the reopen is appended as its own record.
    // =====================================================================

    @Override
    @Transactional
    public FileResponse reopen(UUID id, ReopenFileRequest request) {
        UUID me = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        FileEntity file = findFile(id);

        if (!STATUS_CLOSED.equals(file.getCurrentStatus())) {
            throw new BusinessException("Only a closed file can be reopened.");
        }

        String previousStatus = file.getCurrentStatus();
        UUID previousOwner = file.getCurrentOwnerId();

        // The reopener takes custody so the file lands in their inbox.
        file.setCurrentStatus(STATUS_OPEN);
        file.setCurrentOwnerId(me);
        file.setClosedDate(null);
        fileRepository.save(file);

        FileReopen history = new FileReopen();
        history.setFileId(file.getId());
        history.setReopenedBy(me);
        history.setReopenedAt(LocalDateTime.now());
        history.setReason(request.getReason());
        history.setPreviousStatus(previousStatus);
        reopenRepository.save(history);

        movementService.record(file.getId(), null, null, previousOwner, me,
                file.getCurrentSectionId(), file.getCurrentSectionId(),
                "REOPEN", request.getReason());

        auditService.record(MODULE, "FILE", file.getId(), "REOPEN",
                previousStatus, STATUS_OPEN + " — " + request.getReason());

        return mapper.toFileResponse(file, lookupProvider.load(), List.of(), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriorityChangeResponse> priorityHistory(UUID id) {
        Map<UUID, String> priorities = masterDataFacade.priorityNames();
        Map<UUID, String> users = identityFacade.userNames();
        return priorityChangeRepository
                .findByFileIdAndDeletedFalseOrderByChangedAtDesc(id).stream()
                .map(h -> PriorityChangeResponse.builder()
                        .id(h.getId())
                        .fileId(h.getFileId())
                        .oldPriorityName(h.getOldPriorityId() == null
                                ? null : priorities.get(h.getOldPriorityId()))
                        .newPriorityName(priorities.get(h.getNewPriorityId()))
                        .changedByName(users.get(h.getChangedBy()))
                        .changedAt(h.getChangedAt())
                        .remarks(h.getRemarks())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReopenHistoryResponse> reopenHistory(UUID id) {
        Map<UUID, String> users = identityFacade.userNames();
        return reopenRepository
                .findByFileIdAndDeletedFalseOrderByReopenedAtDesc(id).stream()
                .map(h -> ReopenHistoryResponse.builder()
                        .id(h.getId())
                        .fileId(h.getFileId())
                        .reopenedByName(users.get(h.getReopenedBy()))
                        .reopenedAt(h.getReopenedAt())
                        .previousStatus(h.getPreviousStatus())
                        .reason(h.getReason())
                        .build())
                .toList();
    }
}
