package com.bor.eboard.registry.service.impl;

import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.checklist.dto.ChecklistDtos;
import com.bor.eboard.checklist.service.ChecklistService;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.core.service.NumberGenerationService;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.registry.dto.AttachmentResponse;
import com.bor.eboard.registry.dto.CreateDiaryEntryRequest;
import com.bor.eboard.registry.dto.DiaryEntryResponse;
import com.bor.eboard.registry.dto.DiaryRegisterRow;
import com.bor.eboard.registry.dto.DiaryTrackingEventResponse;
import com.bor.eboard.registry.dto.DiaryFollowupResponse;
import com.bor.eboard.registry.dto.ForwardDiaryRequest;
import com.bor.eboard.registry.dto.UpdateDiaryEntryRequest;
import com.bor.eboard.registry.entity.DiaryEntry;
import com.bor.eboard.registry.entity.ReceiptRegisterEntry;
import com.bor.eboard.registry.mapper.DiaryMapper;
import com.bor.eboard.registry.repository.DiaryEntryRepository;
import com.bor.eboard.registry.repository.ReceiptRegisterRepository;
import com.bor.eboard.registry.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * Registry / Diary implementation.
 *
 * Enforced business rules:
 * - Diary number BOR/{YEAR}/{SEQUENCE}: system-generated, yearly reset,
 *   immutable (06_BUSINESS_RULES.md section 3).
 * - Metadata editable only until first forward (01_PROJECT.md section 9).
 * - A forwarded diary cannot be cancelled or deleted.
 * - Forward = status FORWARDED + inward letter created via
 *   CorrespondenceFacade in the same transaction (02_ARCHITECTURE.md 25/26).
 * - Registry never approves/rejects/closes: no such operation exists here.
 */
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private static final String MODULE = "REGISTRY";
    private static final String STATUS_DIARIZED = "DIARIZED";
    private static final String STATUS_FORWARDED = "FORWARDED";
    private static final String STATUS_RETURNED = "RETURNED_FOR_CORRECTION";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> EDITABLE_STATUSES =
            Set.of("DRAFT", STATUS_DIARIZED, STATUS_RETURNED);


        

    private final DiaryEntryRepository diaryEntryRepository;
    private final ReceiptRegisterRepository receiptRegisterRepository;
    private final NumberGenerationService numberGenerationService;
    private final CorrespondenceFacade correspondenceFacade;
    private final AttachmentStorageService attachmentStorageService;
    private final MasterDataFacade masterDataFacade;
    private final IdentityFacade identityFacade;
    private final DiaryMapper diaryMapper;
    private final AuditService auditService;
    private final ChecklistService checklistService;
    private final com.bor.eboard.notification.facade.NotificationFacade notificationFacade;
    private final com.bor.eboard.workflow.facade.WorkflowEscalationFacade workflowTrackingFacade;

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public DiaryEntryResponse create(CreateDiaryEntryRequest request) {
        validateReferences(request.getCategoryId(), request.getPriorityId(),
                request.getLanguageId(), request.getInitialDepartmentId(),
                request.getInitialSectionId());

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        NumberGenerationService.GeneratedNumber diaryNumber =
                numberGenerationService.next("DIARY", "BOR");
        NumberGenerationService.GeneratedNumber receiptNumber =
                numberGenerationService.next("RECEIPT", "BOR/RCPT");

        DiaryEntry entry = new DiaryEntry();
        entry.setDiaryNumber(diaryNumber.formatted());
        entry.setDiaryYear(diaryNumber.year());
        entry.setDiarySequence(diaryNumber.sequence());
        entry.setSourceType(request.getSourceType());
        entry.setReceivedMode(request.getReceivedMode());
        entry.setReceivedDate(request.getReceivedDate());
        entry.setReceivedBy(currentUserId);
        applyMetadata(entry, request);
        // Barcode/QR carry the diary number for physical labelling
        // (02_ARCHITECTURE.md section 10: generate barcode / QR code).
        entry.setBarcodeValue(diaryNumber.formatted());
        entry.setQrCodeValue(diaryNumber.formatted());
        entry.setStatus(STATUS_DIARIZED);
        entry = diaryEntryRepository.save(entry);

        ReceiptRegisterEntry receipt = new ReceiptRegisterEntry();
        receipt.setDiaryEntryId(entry.getId());
        receipt.setReceiptNumber(receiptNumber.formatted());
        receipt.setReceivedFrom(request.getSenderName());
        receipt.setReceivedBy(currentUserId);
        receipt.setReceivedAt(request.getReceivedDate());
        receipt.setRemarks(request.getRemarks());
        receiptRegisterRepository.save(receipt);
        checklistService.syncForDiary(entry.getId(), entry.getCategoryId());

        auditService.record(MODULE, "DIARY_ENTRY", entry.getId(),
                "CREATE", null,
                "diaryNumber=" + entry.getDiaryNumber()
                        + ", subject=" + truncate(entry.getSubject()));

        return assemble(entry);
    }

    // ------------------------------------------------------------------
    // Update metadata (only until first forward)
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public DiaryEntryResponse updateMetadata(UUID id, UpdateDiaryEntryRequest request) {
        DiaryEntry entry = findEntry(id);
        assertEditable(entry);
        validateReferences(request.getCategoryId(), request.getPriorityId(),
                request.getLanguageId(), request.getInitialDepartmentId(),
                request.getInitialSectionId());

        String oldValue = "subject=" + truncate(entry.getSubject())
                + ", sender=" + entry.getSenderName()
                + ", sectionId=" + entry.getInitialSectionId();

        entry.setSourceType(request.getSourceType());
        entry.setReceivedMode(request.getReceivedMode());
        entry.setReceivedDate(request.getReceivedDate());
        entry.setSenderName(request.getSenderName());
        entry.setSenderDesignation(request.getSenderDesignation());
        entry.setSenderDepartment(request.getSenderDepartment());
        entry.setSenderAddress(request.getSenderAddress());
        entry.setSenderEmail(request.getSenderEmail());
        entry.setSenderMobile(request.getSenderMobile());
        entry.setOriginalLetterNumber(request.getOriginalLetterNumber());
        entry.setReferenceNumber(request.getReferenceNumber());
        entry.setLetterDate(request.getLetterDate());
        entry.setSubject(request.getSubject());
        entry.setDescription(request.getDescription());
        entry.setCategoryId(request.getCategoryId());
        entry.setPriorityId(request.getPriorityId());
        entry.setLanguageId(request.getLanguageId());
        entry.setConfidential(Boolean.TRUE.equals(request.getConfidential()));
        entry.setDueDate(request.getDueDate());
        entry.setReminderDate(request.getReminderDate());
        entry.setPageCount(request.getPageCount());
        entry.setPhysicalCopyReceived(request.getPhysicalCopyReceived() == null
                || request.getPhysicalCopyReceived());
        entry.setInitialDepartmentId(request.getInitialDepartmentId());
        entry.setInitialSectionId(request.getInitialSectionId());
        if (STATUS_RETURNED.equals(entry.getStatus())) {
            entry.setStatus(STATUS_DIARIZED);
        }
        entry = diaryEntryRepository.save(entry);
        checklistService.syncForDiary(entry.getId(), entry.getCategoryId());

        auditService.record(MODULE, "DIARY_ENTRY", entry.getId(),
                "UPDATE", oldValue,
                "subject=" + truncate(entry.getSubject())
                        + ", sender=" + entry.getSenderName()
                        + ", sectionId=" + entry.getInitialSectionId());

        return assemble(entry);
    }

    // ------------------------------------------------------------------
    // Read / search / register
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public DiaryEntryResponse getById(UUID id) {
        return assemble(findEntry(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiaryEntryResponse> search(
            String diaryNumber,
            String sender,
            String subject,
            String status,
            UUID sectionId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            UUID checklistTemplateId,
            String checklistStatus,
            Boolean checklistComplete,
            Boolean missingMandatory,
            PaginationRequest pagination) {

        String normalizedDiaryNumber = StringUtils.hasText(diaryNumber)
                ? "%" + diaryNumber.trim().toLowerCase(Locale.ROOT) + "%"
                : null;

        String normalizedSender = StringUtils.hasText(sender)
                ? "%" + sender.trim().toLowerCase(Locale.ROOT) + "%"
                : null;

        String normalizedSubject = StringUtils.hasText(subject)
                ? "%" + subject.trim().toLowerCase(Locale.ROOT) + "%"
                : null;

        String normalizedStatus = StringUtils.hasText(status)
                ? status.trim()
                : null;

        // Hibernate 6 + PostgreSQL null LocalDateTime workaround
        LocalDateTime normalizedFromDate = fromDate != null
                ? fromDate
                : LocalDateTime.of(1900, 1, 1, 0, 0);

        LocalDateTime normalizedToDate = toDate != null
                ? toDate
                : LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        Page<DiaryEntry> page = diaryEntryRepository.search(
                normalizedDiaryNumber,
                normalizedSender,
                normalizedSubject,
                normalizedStatus,
                sectionId,
                normalizedFromDate,
                normalizedToDate,
                checklistTemplateId != null || StringUtils.hasText(checklistStatus)
                        || checklistComplete != null || missingMandatory != null,
                nonEmptyIds(checklistService.matchingDiaryIds(checklistTemplateId, checklistStatus,
                        checklistComplete, missingMandatory)),
                pagination.toPageable());

        Lookups lookups = loadLookups();

        List<DiaryEntryResponse> content = page.getContent()
                .stream()
                .map(entry -> {
                    DiaryEntryResponse response = diaryMapper.toResponse(
                            entry, lookups.categories(), lookups.priorities(),
                            lookups.languages(), lookups.departments(), lookups.sections(),
                            lookups.users(), null, null);
                    enrichLiveTracking(response, entry, lookups, false);
                    enrichChecklist(response, entry, false);
                    return response;
                })
                .toList();

        return PageResponse.of(content, page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiaryRegisterRow> diaryRegister(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new ValidationException("fromDate and toDate are required");
        }
        if (toDate.isBefore(fromDate)) {
            throw new ValidationException("toDate cannot be before fromDate");
        }
        Lookups lookups = loadLookups();
        return diaryEntryRepository
                .diaryRegister(fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX))
                .stream()
                .map(entry -> DiaryRegisterRow.builder()
                        .id(entry.getId())
                        .diaryNumber(entry.getDiaryNumber())
                        .receivedDate(entry.getReceivedDate())
                        .senderName(entry.getSenderName())
                        .senderDepartment(entry.getSenderDepartment())
                        .originalLetterNumber(entry.getOriginalLetterNumber())
                        .subject(entry.getSubject())
                        .categoryName(entry.getCategoryId() != null
                                ? lookups.categories().get(entry.getCategoryId()) : null)
                        .priorityName(entry.getPriorityId() != null
                                ? lookups.priorities().get(entry.getPriorityId()) : null)
                        .initialSectionName(entry.getInitialSectionId() != null
                                ? lookups.sections().get(entry.getInitialSectionId()) : null)
                        .status(entry.getStatus())
                        .build())
                .toList();
    }

    // ------------------------------------------------------------------
    // Forward
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public DiaryEntryResponse forward(UUID id, ForwardDiaryRequest request) {
        DiaryEntry entry = findEntry(id);
        if (!EDITABLE_STATUSES.contains(entry.getStatus())) {
            throw new BusinessException(
                    "Diary entry cannot be forwarded in status " + entry.getStatus());
        }

        IdentityFacade.SectionRef section = identityFacade
                .findSection(request.getSectionId())
                .orElseThrow(() -> new ValidationException("Invalid target section"));
        if (request.getUserId() != null) {
            IdentityFacade.UserRef target = identityFacade.findUser(request.getUserId())
                    .orElseThrow(() -> new ValidationException("Invalid target user"));
            if (target.sectionId() == null
                    || !target.sectionId().equals(section.id())) {
                throw new ValidationException(
                        "Target user does not belong to the target section");
            }
        }

        checklistService.validateDiaryForward(entry.getId(),
                Boolean.TRUE.equals(request.getChecklistOverride()),
                request.getChecklistOverrideRemarks());

        entry.setInitialSectionId(section.id());
        entry.setInitialDepartmentId(section.departmentId());
        entry.setInitialAssignedUserId(request.getUserId());
        entry.setStatus(STATUS_FORWARDED);
        entry = diaryEntryRepository.save(entry);

        // Same transaction: forwarded diary always has its inward letter
        // (02_ARCHITECTURE.md section 26).
        UUID letterId = correspondenceFacade.createInwardLetterFromDiary(
                new CorrespondenceFacade.InwardLetterCommand(
                        entry.getId(),
                        entry.getOriginalLetterNumber(),
                        entry.getReferenceNumber(),
                        entry.getLetterDate(),
                        entry.getSubject(),
                        entry.getDescription(),
                        entry.getSenderName(),
                        entry.getSenderDesignation(),
                        entry.getSenderDepartment(),
                        entry.getSenderAddress(),
                        entry.getInitialDepartmentId(),
                        entry.getInitialSectionId(),
                        entry.getInitialAssignedUserId(),
                        entry.getCategoryId(),
                        entry.getPriorityId(),
                        entry.getLanguageId(),
                        Boolean.TRUE.equals(entry.getConfidential()),
                        entry.getDueDate(),
                        entry.getReminderDate()));
  

        auditService.record(MODULE, "DIARY_ENTRY", entry.getId(),
                "FORWARD", null,
                "diaryNumber=" + entry.getDiaryNumber()
                        + ", toSectionId=" + section.id()
                        + (request.getUserId() != null
                                ? ", toUserId=" + request.getUserId() : "")
                        + ", letterId=" + letterId
                        + (request.getRemarks() != null
                                ? ", remarks=" + truncate(request.getRemarks()) : ""));

        // Notify the assigned officer, when the diary was forwarded to a
        // specific user rather than to a whole section.
        if (request.getUserId() != null) {
            java.util.Map<String, String> values = new java.util.HashMap<>();
            values.put("fileNumber", entry.getDiaryNumber());
            values.put("subject", entry.getSubject() != null ? entry.getSubject() : "");
            values.put("remarks", request.getRemarks() != null ? request.getRemarks() : "");
            notificationFacade.notifyFromTemplate(request.getUserId(), "ASSIGNMENT",
                    values, "DIARY", entry.getId(), null);
        }

        return assemble(entry);
    }

    // ------------------------------------------------------------------
    // Cancel (before forward only)
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public DiaryEntryResponse cancel(UUID id, String reason) {
        DiaryEntry entry = findEntry(id);
        if (!EDITABLE_STATUSES.contains(entry.getStatus())) {
            throw new BusinessException(
                    "A diary entry cannot be cancelled after forwarding");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Cancellation reason is required");
        }
        String oldStatus = entry.getStatus();
        entry.setStatus(STATUS_CANCELLED);
        entry = diaryEntryRepository.save(entry);
        auditService.record(MODULE, "DIARY_ENTRY", entry.getId(),
                "CANCEL", oldStatus,
                STATUS_CANCELLED + ", reason=" + truncate(reason));
        return assemble(entry);
    }

    // ------------------------------------------------------------------
    // Attachments
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public AttachmentResponse uploadAttachment(UUID diaryId, UUID documentTypeId,
                                               MultipartFile file) {
        DiaryEntry entry = findEntry(diaryId);
        if (!EDITABLE_STATUSES.contains(entry.getStatus())) {
            throw new BusinessException(
                    "Diary attachments are locked after forwarding or cancellation");
        }
        AttachmentStorageService.StoredAttachment stored =
                attachmentStorageService.store("DIARY", entry.getId(), documentTypeId, file);
        return diaryMapper.toAttachment(stored);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private DiaryEntry findEntry(UUID id) {
        return diaryEntryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diary entry", id));
    }

    private void assertEditable(DiaryEntry entry) {
        if (!EDITABLE_STATUSES.contains(entry.getStatus())) {
            throw new BusinessException(
                    "Diary metadata is locked after the first forward (status: "
                            + entry.getStatus() + ")");
        }
    }

    private void applyMetadata(DiaryEntry entry, CreateDiaryEntryRequest request) {
        entry.setSenderName(request.getSenderName());
        entry.setSenderDesignation(request.getSenderDesignation());
        entry.setSenderDepartment(request.getSenderDepartment());
        entry.setSenderAddress(request.getSenderAddress());
        entry.setSenderEmail(request.getSenderEmail());
        entry.setSenderMobile(request.getSenderMobile());
        entry.setOriginalLetterNumber(request.getOriginalLetterNumber());
        entry.setReferenceNumber(request.getReferenceNumber());
        entry.setLetterDate(request.getLetterDate());
        entry.setSubject(request.getSubject());
        entry.setDescription(request.getDescription());
        entry.setCategoryId(request.getCategoryId());
        entry.setPriorityId(request.getPriorityId());
        entry.setLanguageId(request.getLanguageId());
        entry.setConfidential(Boolean.TRUE.equals(request.getConfidential()));
        entry.setDueDate(request.getDueDate());
        entry.setReminderDate(request.getReminderDate());
        entry.setPageCount(request.getPageCount());
        entry.setPhysicalCopyReceived(request.getPhysicalCopyReceived() == null
                || request.getPhysicalCopyReceived());
        entry.setInitialDepartmentId(request.getInitialDepartmentId());
        entry.setInitialSectionId(request.getInitialSectionId());
    }

    private void validateReferences(UUID categoryId, UUID priorityId, UUID languageId,
                                    UUID departmentId, UUID sectionId) {
        if (!masterDataFacade.categoryExists(categoryId)) {
            throw new ValidationException("Invalid category reference");
        }
        if (!masterDataFacade.priorityExists(priorityId)) {
            throw new ValidationException("Invalid priority reference");
        }
        if (languageId != null && !masterDataFacade.languageExists(languageId)) {
            throw new ValidationException("Invalid language reference");
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

    private DiaryEntryResponse assemble(DiaryEntry entry) {
        Lookups lookups = loadLookups();
        ReceiptRegisterEntry receipt =
                receiptRegisterRepository.findByDiaryEntryId(entry.getId()).orElse(null);
        List<AttachmentStorageService.StoredAttachment> attachments =
                attachmentStorageService.listFor("DIARY", entry.getId());
        DiaryEntryResponse response = diaryMapper.toResponse(entry,
                lookups.categories(), lookups.priorities(), lookups.languages(),
                lookups.departments(), lookups.sections(), lookups.users(),
                receipt, attachments);

        enrichLiveTracking(response, entry, lookups, true);
        enrichChecklist(response, entry, true);
        if (response.getWorkflowAttachments() == null) response.setWorkflowAttachments(List.of());
        if (response.getMovementHistory() == null) response.setMovementHistory(List.of());
        if (response.getTimeline() == null) response.setTimeline(List.of());
        if (response.getFollowups() == null) response.setFollowups(List.of());
        return response;
    }

    private void enrichLiveTracking(DiaryEntryResponse response, DiaryEntry entry,
                                    Lookups lookups, boolean includeDetails) {
        correspondenceFacade.findDiaryLetterState(entry.getId()).ifPresent(letter -> {
            response.setLinkedLetterId(letter.letterId());
            response.setCurrentOwnerId(letter.currentOwnerId());
            response.setCurrentOwnerName(letter.currentOwnerId() != null ? lookups.users().get(letter.currentOwnerId()) : null);
            response.setCurrentSectionId(letter.currentSectionId());
            response.setCurrentSectionName(letter.currentSectionId() != null ? lookups.sections().get(letter.currentSectionId()) : null);
            response.setLinkedLetterStatus(letter.currentStatus());
            response.setLinkedLetterDueDate(letter.dueDate());
            boolean disposed = "CLOSED".equals(letter.currentStatus()) || "ARCHIVED".equals(letter.currentStatus());
            response.setFinalDisposalStatus(disposed ? "DISPOSED" : "PENDING");
            response.setDisposedAt(disposed ? letter.updatedAt() : null);

            workflowTrackingFacade.currentLetterTask(letter.letterId()).ifPresent(task -> {
                response.setWorkflowInstanceId(task.workflowInstanceId());
                response.setWorkflowStatus(task.workflowStatus());
                response.setWorkflowStageId(task.stepId());
                response.setWorkflowStageOrder(task.stepOrder());
                response.setWorkflowStage(task.stepName());
                response.setPendingSince(task.assignedAt());
                LocalDate due = task.dueDate() != null ? task.dueDate() : letter.dueDate();
                response.setLinkedLetterDueDate(due);
                applySla(response, due);
                response.setEscalationStatus(notificationFacade.latestEscalationLevel(task.taskId()).orElse("NONE"));
            });
            if (response.getSlaStatus() == null) applySla(response, letter.dueDate());
            if (response.getEscalationStatus() == null) response.setEscalationStatus("NONE");

            if (includeDetails) {
                response.setWorkflowAttachments(attachmentStorageService.listFor("LETTER", letter.letterId()).stream()
                        .map(diaryMapper::toAttachment).toList());
                response.setFollowups(correspondenceFacade.letterFollowups(letter.letterId()).stream()
                        .map(x -> DiaryFollowupResponse.builder().id(x.id()).followupDate(x.followupDate())
                                .followupType(x.followupType()).dueDate(x.dueDate()).reminderDate(x.reminderDate())
                                .nextFollowupDate(x.nextFollowupDate()).replyReceived(x.replyReceived())
                                .replyReceivedDate(x.replyReceivedDate()).status(x.status()).remarks(x.remarks())
                                .createdAt(x.createdAt()).build()).toList());
                buildTrackingHistory(response, entry, letter.letterId(), lookups);
            }
        });
    }

    private void enrichChecklist(DiaryEntryResponse response, DiaryEntry entry, boolean includeDetails) {
        checklistService.getSummaryForDiary(entry.getId()).ifPresent(response::setChecklistSummary);
        if (!includeDetails) return;
        checklistService.getForDiary(entry.getId()).ifPresent(checklist -> {
            response.setChecklist(checklist);
            List<DiaryTrackingEventResponse> timeline = new ArrayList<>(
                    response.getTimeline() != null ? response.getTimeline() : List.of());
            if (checklist.getTimeline() != null) {
                checklist.getTimeline().forEach(event -> timeline.add(DiaryTrackingEventResponse.builder()
                        .timestamp(event.getTimestamp()).eventType("CHECKLIST")
                        .action(event.getAction()).title(event.getTitle()).detail(event.getDetail())
                        .referenceId(event.getItemId() != null ? event.getItemId() : checklist.getId())
                        .toUserId(event.getUserId()).toUserName(event.getUserName()).build()));
            }
            timeline.sort(Comparator.comparing(DiaryTrackingEventResponse::getTimestamp,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            response.setTimeline(timeline);
        });
    }

    private List<UUID> nonEmptyIds(List<UUID> ids) {
        return ids == null || ids.isEmpty()
                ? List.of(new UUID(0L, 0L)) : ids;
    }

    private void applySla(DiaryEntryResponse response, LocalDate dueDate) {
        if (dueDate == null) {
            response.setSlaStatus("NOT_SET");
            return;
        }
        long remaining = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        response.setRemainingDays(Math.max(remaining, 0));
        response.setOverdueDays(Math.max(-remaining, 0));
        response.setSlaStatus(remaining < 0 ? "OVERDUE" : remaining == 0 ? "DUE_TODAY"
                : remaining <= 2 ? "DUE_SOON" : "ON_TRACK");
    }

    private void buildTrackingHistory(DiaryEntryResponse response, DiaryEntry entry, UUID letterId, Lookups lookups) {
        List<DiaryTrackingEventResponse> movements = new ArrayList<>();
        for (var m : correspondenceFacade.letterMovements(letterId)) {
            movements.add(event(m.actionAt(), "LETTER_MOVEMENT", m.action(), m.remarks(), m.id(),
                    m.fromUserId(), m.toUserId(), m.fromSectionId(), m.toSectionId(), lookups));
        }
        for (var m : workflowTrackingFacade.letterMovementHistory(letterId)) {
            movements.add(event(m.actionAt(), "WORKFLOW_MOVEMENT", m.action(), m.remarks(), m.id(),
                    m.fromUserId(), m.toUserId(), m.fromSectionId(), m.toSectionId(), lookups));
        }
        movements.sort(Comparator.comparing(DiaryTrackingEventResponse::getTimestamp));
        response.setMovementHistory(movements);

        List<DiaryTrackingEventResponse> timeline = new ArrayList<>();
        timeline.add(DiaryTrackingEventResponse.builder().timestamp(entry.getCreatedAt())
                .eventType("DIARY").action("REGISTERED").title("Diary registered")
                .detail(entry.getDiaryNumber()).referenceId(entry.getId()).build());
        if (STATUS_FORWARDED.equals(entry.getStatus())) {
            timeline.add(DiaryTrackingEventResponse.builder().timestamp(entry.getUpdatedAt())
                    .eventType("DIARY").action("FORWARDED").title("Diary forwarded")
                    .detail("Linked Letter created").referenceId(letterId).build());
        }
        timeline.addAll(movements);
        if (response.getFollowups() != null) {
            response.getFollowups().forEach(x -> timeline.add(DiaryTrackingEventResponse.builder()
                    .timestamp(x.getCreatedAt()).eventType("FOLLOWUP").action(x.getStatus())
                    .title("Follow-up " + (x.getFollowupType() != null ? x.getFollowupType() : "recorded"))
                    .detail(x.getRemarks()).referenceId(x.getId()).build()));
        }
        timeline.sort(Comparator.comparing(DiaryTrackingEventResponse::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        response.setTimeline(timeline);
    }

    private DiaryTrackingEventResponse event(LocalDateTime at, String type, String action, String remarks,
                                             UUID referenceId, UUID fromUser, UUID toUser,
                                             UUID fromSection, UUID toSection, Lookups lookups) {
        return DiaryTrackingEventResponse.builder().timestamp(at).eventType(type).action(action)
                .title(action != null ? action.replace('_', ' ') : type).detail(remarks).referenceId(referenceId)
                .fromUserId(fromUser).fromUserName(fromUser != null ? lookups.users().get(fromUser) : null)
                .toUserId(toUser).toUserName(toUser != null ? lookups.users().get(toUser) : null)
                .fromSectionId(fromSection).fromSectionName(fromSection != null ? lookups.sections().get(fromSection) : null)
                .toSectionId(toSection).toSectionName(toSection != null ? lookups.sections().get(toSection) : null)
                .build();
    }

    private record Lookups(Map<UUID, String> categories,
                           Map<UUID, String> priorities,
                           Map<UUID, String> languages,
                           Map<UUID, String> departments,
                           Map<UUID, String> sections,
                           Map<UUID, String> users) {
    }

    private Lookups loadLookups() {
        return new Lookups(
                masterDataFacade.categoryNames(),
                masterDataFacade.priorityNames(),
                masterDataFacade.languageNames(),
                identityFacade.departmentNames(),
                identityFacade.sectionNames(),
                identityFacade.userNames());
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
}
