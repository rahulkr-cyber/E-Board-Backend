package com.bor.eboard.correspondence.service.impl;

import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.checklist.service.ChecklistService;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.dto.AttachmentInfo;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.dto.CreateLetterRequest;
import com.bor.eboard.correspondence.dto.LetterMovementResponse;
import com.bor.eboard.correspondence.dto.LetterActionRequest;
import com.bor.eboard.correspondence.dto.MarkLetterRequest;
import com.bor.eboard.correspondence.dto.UpdateLetterRequest;
import com.bor.eboard.correspondence.entity.LetterMovement;
import com.bor.eboard.correspondence.repository.LetterMovementRepository;
import com.bor.eboard.correspondence.dto.LetterResponse;
import com.bor.eboard.correspondence.entity.FileEntity;
import com.bor.eboard.correspondence.entity.Letter;
import com.bor.eboard.correspondence.mapper.CorrespondenceLookups;
import com.bor.eboard.correspondence.mapper.CorrespondenceMapper;
import com.bor.eboard.correspondence.mapper.LookupProvider;
import com.bor.eboard.correspondence.repository.FileRepository;
import com.bor.eboard.correspondence.repository.LetterRepository;
import com.bor.eboard.correspondence.service.LetterService;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Letter implementation.
 *
 * Enforced rules (06_BUSINESS_RULES.md section 5):
 * - A non-draft letter must belong to a file.
 * - Letters cannot be added to a closed/archived file.
 * - New outward/internal letters start in status DRAFT (inward letters are
 *   created by the Registry facade at RECEIVED).
 */
@Service
@RequiredArgsConstructor
public class LetterServiceImpl implements LetterService {

    private static final String MODULE = "CORRESPONDENCE";
    private static final Set<String> LOCKED = Set.of("CLOSED", "ARCHIVED");

    private final LetterRepository letterRepository;
    private final FileRepository fileRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final MasterDataFacade masterDataFacade;
    private final CorrespondenceMapper mapper;
    private final LookupProvider lookupProvider;
    private final AuditService auditService;
    private final ChecklistService checklistService;
    // BCR-03 Part 16
    private final LetterMovementRepository letterMovementRepository;
    private final com.bor.eboard.identity.facade.IdentityFacade identityFacade;
    private final com.bor.eboard.assignment.facade.AssignmentFacade assignmentFacade;
    private final com.bor.eboard.notification.facade.NotificationFacade notificationFacade;

    @Override
    @Transactional
    public LetterResponse create(CreateLetterRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        FileEntity file = null;
        if (request.getFileId() != null) {
            file = fileRepository.findByIdAndDeletedFalse(request.getFileId())
                    .orElseThrow(() -> new ValidationException("Invalid file reference"));
            if (LOCKED.contains(file.getCurrentStatus())) {
                throw new BusinessException(
                        "Letters cannot be added to a " + file.getCurrentStatus() + " file");
            }
        } else if (!request.isDraft()) {
            throw new ValidationException(
                    "A letter must belong to a file unless saved as a draft");
        }

        if (request.getCategoryId() != null && !masterDataFacade.categoryExists(request.getCategoryId())) {
            throw new ValidationException("Invalid category reference");
        }
        if (request.getPriorityId() != null && !masterDataFacade.priorityExists(request.getPriorityId())) {
            throw new ValidationException("Invalid priority reference");
        }
        if (request.getLanguageId() != null && !masterDataFacade.languageExists(request.getLanguageId())) {
            throw new ValidationException("Invalid language reference");
        }

        Letter letter = new Letter();
        letter.setFileId(request.getFileId());
        letter.setLetterDirection(request.getLetterDirection());
        letter.setLetterType(request.getLetterType());
        letter.setLetterNumber(request.getLetterNumber());
        letter.setReferenceNumber(request.getReferenceNumber());
        letter.setLetterDate(request.getLetterDate());
        letter.setSubject(request.getSubject());
        letter.setBody(request.getBody());
        letter.setSenderName(request.getSenderName());
        letter.setSenderDesignation(request.getSenderDesignation());
        letter.setSenderDepartment(request.getSenderDepartment());
        letter.setSenderAddress(request.getSenderAddress());
        letter.setReceiverDepartmentId(request.getReceiverDepartmentId());
        letter.setReceiverSectionId(request.getReceiverSectionId());
        letter.setReceiverUserId(request.getReceiverUserId());
        letter.setCategoryId(request.getCategoryId());
        letter.setPriorityId(request.getPriorityId());
        letter.setLanguageId(request.getLanguageId());
        letter.setConfidential(Boolean.TRUE.equals(request.getConfidential()));
        letter.setDueDate(request.getDueDate());
        letter.setReminderDate(request.getReminderDate());
        letter.setCurrentOwnerId(currentUserId);
        letter.setCurrentSectionId(file != null ? file.getCurrentSectionId()
                : request.getReceiverSectionId());
        // BCR-03 Part 16: a newly created letter is visible only to its creator.
        // It stays in the creator's Drafts until explicitly marked onward.
        letter.setCurrentStatus("DRAFT");
        letter = letterRepository.save(letter);

        auditService.record(MODULE, "LETTER", letter.getId(), "CREATE", null,
                "direction=" + letter.getLetterDirection()
                        + ", type=" + letter.getLetterType()
                        + (letter.getFileId() != null ? ", fileId=" + letter.getFileId() : ", draft"));

        return assemble(letter);
    }

    @Override
    @Transactional(readOnly = true)
    public LetterResponse getById(UUID id) {
        return assemble(findLetter(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LetterResponse> listByFile(UUID fileId) {
        CorrespondenceLookups lookups = lookupProvider.load();
        String fileNumber = fileRepository.findByIdAndDeletedFalse(fileId)
                .map(FileEntity::getFileNumber).orElse(null);
        return letterRepository.findByFileIdAndDeletedFalseOrderByCreatedAtAsc(fileId).stream()
                .map(letter -> mapper.toLetterResponse(letter, fileNumber, lookups,
                        attachmentsFor(letter.getId())))
                .toList();
    }

    @Override
    @Transactional
    public AttachmentInfo uploadAttachment(UUID letterId, UUID documentTypeId, MultipartFile file) {
        Letter letter = findLetter(letterId);
        AttachmentStorageService.StoredAttachment stored =
                attachmentStorageService.store("LETTER", letter.getId(), documentTypeId, file);
        return mapper.toAttachmentInfo(stored);
    }

    // ------------------------------------------------------------------

    private Letter findLetter(UUID id) {
        return letterRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Letter", id));
    }

    private LetterResponse assemble(Letter letter) {
        CorrespondenceLookups lookups = lookupProvider.load();
        String fileNumber = letter.getFileId() != null
                ? fileRepository.findByIdAndDeletedFalse(letter.getFileId())
                        .map(FileEntity::getFileNumber).orElse(null)
                : null;
        LetterResponse response = mapper.toLetterResponse(letter, fileNumber, lookups,
                attachmentsFor(letter.getId()));
        checklistService.getSummaryForLetter(letter.getId()).ifPresent(response::setChecklistSummary);
        checklistService.getForLetter(letter.getId()).ifPresent(response::setChecklist);
        return response;
    }

    private List<AttachmentInfo> attachmentsFor(UUID letterId) {
        return attachmentStorageService.listFor("LETTER", letterId).stream()
                .map(mapper::toAttachmentInfo).toList();
    }

    // =====================================================================
    // BCR-03 Part 16: letter boxes and the Mark action. Identical ownership
    // model to files — no user sees all letters by default.
    // =====================================================================

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<LetterResponse> box(String box, PaginationRequest pagination) {
        UUID me = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        org.springframework.data.domain.Pageable pageable = pagination.toPageable();
        String key = box == null ? "" : box.trim().toLowerCase();

        org.springframework.data.domain.Page<Letter> page = switch (key) {
            case "drafts" -> letterRepository.boxDrafts(me, pageable);
            case "inbox" -> letterRepository.boxInbox(me, pageable);
            case "returned" -> letterRepository.boxReturned(me, pageable);
            case "closed" -> letterRepository.boxClosed(me, pageable);
            case "sent" -> {
                List<UUID> moved = letterMovementRepository.findLetterIdsMovedBy(me);
                yield moved.isEmpty()
                        ? org.springframework.data.domain.Page.empty(pageable)
                        : letterRepository.boxSent(me, moved, pageable);
            }
            default -> throw new ValidationException(
                    "Unknown box '" + box + "'. Use drafts, inbox, sent, returned or closed.");
        };

        List<LetterResponse> content = page.getContent().stream()
                .map(this::assemble)
                .toList();
        return PageResponse.of(content, page);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public LetterResponse mark(UUID id, MarkLetterRequest request) {
        UUID me = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        Letter letter = letterRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new com.bor.eboard.common.exception
                        .ResourceNotFoundException("Letter", id));

        if (!me.equals(letter.getCurrentOwnerId())) {
            throw new ForbiddenException("Only the current owner may mark this letter.");
        }
        if ("CLOSED".equals(letter.getCurrentStatus())
                || "ARCHIVED".equals(letter.getCurrentStatus())) {
            throw new com.bor.eboard.common.exception.BusinessException(
                    "A closed letter cannot be marked.");
        }
        if (me.equals(request.getToUserId())) {
            throw new ValidationException("A letter cannot be marked to yourself.");
        }

        com.bor.eboard.identity.facade.IdentityFacade.UserRef target =
                identityFacade.findUser(request.getToUserId())
                        .orElseThrow(() -> new ValidationException("Invalid target user"));
        if (!target.active()) {
            throw new ValidationException("The target user is not active.");
        }

        UUID toSection = request.getToSectionId() != null
                ? request.getToSectionId() : target.sectionId();

        List<UUID> myRoles = identityFacade.activeRoleIdsOf(me);
        if (!assignmentFacade.canMark(myRoles, letter.getCurrentSectionId(),
                request.getToUserId(), toSection)) {
            throw new ForbiddenException(
                    "No assignment rule permits you to mark this letter to that user.");
        }

        checklistService.validateLetterForward(letter.getId(),
                Boolean.TRUE.equals(request.getChecklistOverride()),
                request.getChecklistOverrideRemarks());

        UUID fromUser = letter.getCurrentOwnerId();
        UUID fromSection = letter.getCurrentSectionId();

        letter.setCurrentOwnerId(request.getToUserId());
        letter.setCurrentSectionId(toSection);
        letter.setCurrentStatus("IN_PROGRESS");
        letterRepository.save(letter);

        LetterMovement movement = new LetterMovement();
        movement.setLetterId(letter.getId());
        movement.setAction("MARK");
        movement.setFromUserId(fromUser);
        movement.setFromSectionId(fromSection);
        movement.setToUserId(request.getToUserId());
        movement.setToSectionId(toSection);
        movement.setRemarks(request.getRemarks());
        movement.setActionAt(java.time.LocalDateTime.now());
        movement.setActionBy(me);
        letterMovementRepository.save(movement);

        auditService.record(MODULE, "LETTER", letter.getId(), "MARK",
                null, "Marked to " + target.fullName());

        notificationFacade.notify(request.getToUserId(),
                "Letter marked to you",
                "Letter " + (letter.getLetterNumber() == null ? "" : letter.getLetterNumber())
                        + " — " + letter.getSubject(),
                "TASK_ASSIGNED", "LETTER", letter.getId(), "NORMAL");

        return assemble(letter);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<LetterMovementResponse> movements(UUID id) {
        java.util.Map<UUID, String> users = identityFacade.userNames();
        java.util.Map<UUID, String> sections = identityFacade.sectionNames();
        return letterMovementRepository
                .findByLetterIdAndDeletedFalseOrderByActionAtAsc(id).stream()
                .map(m -> LetterMovementResponse.builder()
                        .id(m.getId())
                        .action(m.getAction())
                        .fromUserName(m.getFromUserId() == null ? null : users.get(m.getFromUserId()))
                        .toUserName(m.getToUserId() == null ? null : users.get(m.getToUserId()))
                        .fromSectionName(m.getFromSectionId() == null ? null : sections.get(m.getFromSectionId()))
                        .toSectionName(m.getToSectionId() == null ? null : sections.get(m.getToSectionId()))
                        .remarks(m.getRemarks())
                        .actionAt(m.getActionAt())
                        .build())
                .toList();
    }

    // =====================================================================
    // Letter lifecycle: update, return, close, reopen, note, search.
    //
    // LetterMovement already declared MARK | RETURN | CLOSE | REOPEN — the
    // trail was designed for this; only MARK had been wired. Each action below
    // appends an immutable movement (the DB trigger forbids any other outcome),
    // audits, and where relevant notifies.
    // =====================================================================

    @Override
    @Transactional
    public LetterResponse update(UUID id, UpdateLetterRequest request) {
        UUID me = requireUser();
        Letter letter = findLetter(id);

        if (!me.equals(letter.getCurrentOwnerId())) {
            throw new ForbiddenException("Only the current holder may correct this letter.");
        }
        // A letter that has been marked onward has been seen by someone else;
        // silently rewriting its subject or body would falsify the record.
        if (!"DRAFT".equals(letter.getCurrentStatus())) {
            throw new BusinessException(
                    "Only a draft letter can be corrected. This letter has already been marked.");
        }

        validateMasters(request.getCategoryId(), request.getPriorityId(), request.getLanguageId());

        letter.setLetterNumber(request.getLetterNumber());
        letter.setReferenceNumber(request.getReferenceNumber());
        letter.setLetterDate(request.getLetterDate());
        letter.setSubject(request.getSubject());
        letter.setBody(request.getBody());
        letter.setSenderName(request.getSenderName());
        letter.setSenderDesignation(request.getSenderDesignation());
        letter.setSenderDepartment(request.getSenderDepartment());
        letter.setSenderAddress(request.getSenderAddress());
        letter.setReceiverDepartmentId(request.getReceiverDepartmentId());
        letter.setReceiverSectionId(request.getReceiverSectionId());
        letter.setReceiverUserId(request.getReceiverUserId());
        letter.setCategoryId(request.getCategoryId());
        letter.setPriorityId(request.getPriorityId());
        letter.setLanguageId(request.getLanguageId());
        letter.setConfidential(Boolean.TRUE.equals(request.getConfidential()));
        letter.setDueDate(request.getDueDate());
        letter.setReminderDate(request.getReminderDate());
        letterRepository.save(letter);

        auditService.record(MODULE, "LETTER", letter.getId(), "UPDATE", null,
                "Draft letter corrected");

        return assemble(letter);
    }

    @Override
    @Transactional
    public LetterResponse returnLetter(UUID id, LetterActionRequest request) {
        UUID me = requireUser();
        Letter letter = findLetter(id);

        if (!me.equals(letter.getCurrentOwnerId())) {
            throw new ForbiddenException("Only the current holder may return this letter.");
        }
        if (LOCKED.contains(letter.getCurrentStatus())) {
            throw new BusinessException("A closed letter cannot be returned. Reopen it first.");
        }

        // Return it to whoever last marked it to me.
        LetterMovement last = letterMovementRepository
                .findByLetterIdAndDeletedFalseOrderByActionAtAsc(id).stream()
                .filter(m -> me.equals(m.getToUserId()) && m.getFromUserId() != null)
                .reduce((first, second) -> second)     // the most recent
                .orElseThrow(() -> new BusinessException(
                        "This letter has no previous holder to return it to."));

        UUID toUser = last.getFromUserId();
        UUID toSection = last.getFromSectionId();
        UUID fromSection = letter.getCurrentSectionId();

        letter.setCurrentOwnerId(toUser);
        letter.setCurrentSectionId(toSection);
        letter.setCurrentStatus("RETURNED");
        letterRepository.save(letter);

        recordMovement(letter, "RETURN", me, fromSection, toUser, toSection, request.getRemarks());

        auditService.record(MODULE, "LETTER", letter.getId(), "RETURN", null, request.getRemarks());

        notificationFacade.notify(toUser,
                "Letter returned to you",
                "Letter " + describe(letter) + " was returned: " + request.getRemarks(),
                "RETURN", "LETTER", letter.getId(), "HIGH");

        return assemble(letter);
    }

    @Override
    @Transactional
    public LetterResponse close(UUID id, LetterActionRequest request) {
        UUID me = requireUser();
        Letter letter = findLetter(id);

        if (!me.equals(letter.getCurrentOwnerId())) {
            throw new ForbiddenException("Only the current holder may close this letter.");
        }
        if (LOCKED.contains(letter.getCurrentStatus())) {
            throw new BusinessException("This letter is already closed.");
        }
        if ("DRAFT".equals(letter.getCurrentStatus())) {
            throw new BusinessException(
                    "A draft letter cannot be closed — it has never entered correspondence.");
        }

        String previous = letter.getCurrentStatus();
        letter.setCurrentStatus("CLOSED");
        letterRepository.save(letter);

        recordMovement(letter, "CLOSE", me, letter.getCurrentSectionId(),
                null, null, request.getRemarks());

        auditService.record(MODULE, "LETTER", letter.getId(), "CLOSE",
                previous, "CLOSED — " + request.getRemarks());

        return assemble(letter);
    }

    @Override
    @Transactional
    public LetterResponse reopen(UUID id, LetterActionRequest request) {
        UUID me = requireUser();
        Letter letter = findLetter(id);

        if (!LOCKED.contains(letter.getCurrentStatus())) {
            throw new BusinessException("Only a closed letter can be reopened.");
        }

        String previous = letter.getCurrentStatus();
        UUID previousOwner = letter.getCurrentOwnerId();

        // The reopener takes custody, so it lands in their inbox.
        letter.setCurrentStatus("IN_PROGRESS");
        letter.setCurrentOwnerId(me);
        letterRepository.save(letter);

        recordMovement(letter, "REOPEN", me, letter.getCurrentSectionId(),
                me, letter.getCurrentSectionId(), request.getRemarks());

        auditService.record(MODULE, "LETTER", letter.getId(), "REOPEN",
                previous, "IN_PROGRESS — " + request.getRemarks());

        if (previousOwner != null && !previousOwner.equals(me)) {
            notificationFacade.notify(previousOwner,
                    "Letter reopened",
                    "Letter " + describe(letter) + " was reopened: " + request.getRemarks(),
                    "ASSIGNMENT", "LETTER", letter.getId(), "NORMAL");
        }

        return assemble(letter);
    }

    @Override
    @Transactional
    public LetterMovementResponse addNote(UUID id, LetterActionRequest request) {
        UUID me = requireUser();
        Letter letter = findLetter(id);

        // A note records a remark without moving the letter — the holder does
        // not change, so from and to are the same.
        LetterMovement note = recordMovement(letter, "NOTE", me,
                letter.getCurrentSectionId(), letter.getCurrentOwnerId(),
                letter.getCurrentSectionId(), request.getRemarks());

        auditService.record(MODULE, "LETTER", letter.getId(), "NOTE", null, request.getRemarks());

        Map<UUID, String> users = identityFacade.userNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        return LetterMovementResponse.builder()
                .id(note.getId())
                .action(note.getAction())
                .fromUserName(note.getFromUserId() == null ? null : users.get(note.getFromUserId()))
                .toUserName(note.getToUserId() == null ? null : users.get(note.getToUserId()))
                .fromSectionName(note.getFromSectionId() == null ? null : sections.get(note.getFromSectionId()))
                .toSectionName(note.getToSectionId() == null ? null : sections.get(note.getToSectionId()))
                .remarks(note.getRemarks())
                .actionAt(note.getActionAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LetterResponse> search(String letterNumber, String subject,
                                               String direction, String letterType,
                                               String status, UUID sectionId, UUID fileId,
                                               UUID checklistTemplateId, String checklistStatus,
                                               Boolean checklistComplete, Boolean missingMandatory,
                                               PaginationRequest pagination) {
    	
    	String normalizedLetterNumber =
    	        StringUtils.hasText(letterNumber)
    	                ? "%" + letterNumber.trim().toLowerCase(Locale.ROOT) + "%"
    	                : null;

    	String normalizedSubject =
    	        StringUtils.hasText(subject)
    	                ? "%" + subject.trim().toLowerCase(Locale.ROOT) + "%"
    	                : null;
        org.springframework.data.domain.Page<Letter> page = letterRepository.search(
        		normalizedLetterNumber, normalizedSubject, blankToNull(direction),
                blankToNull(letterType), blankToNull(status), sectionId, fileId,
                checklistTemplateId != null || StringUtils.hasText(checklistStatus)
                        || checklistComplete != null || missingMandatory != null,
                nonEmptyIds(checklistService.matchingLetterIds(checklistTemplateId, checklistStatus,
                        checklistComplete, missingMandatory)),
                pagination.toPageable());

        List<LetterResponse> content = page.getContent().stream()
                .map(this::assemble)
                .toList();
        return PageResponse.of(content, page);
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private UUID requireUser() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }

    private LetterMovement recordMovement(Letter letter, String action, UUID actor,
                                          UUID fromSection, UUID toUser, UUID toSection,
                                          String remarks) {
        LetterMovement movement = new LetterMovement();
        movement.setLetterId(letter.getId());
        movement.setAction(action);
        movement.setFromUserId(actor);
        movement.setFromSectionId(fromSection);
        movement.setToUserId(toUser);
        movement.setToSectionId(toSection);
        movement.setRemarks(remarks);
        movement.setActionAt(java.time.LocalDateTime.now());
        movement.setActionBy(actor);
        return letterMovementRepository.save(movement);
    }

    private void validateMasters(UUID categoryId, UUID priorityId, UUID languageId) {
        if (categoryId != null && !masterDataFacade.categoryExists(categoryId)) {
            throw new ValidationException("Invalid categoryId");
        }
        if (priorityId != null && !masterDataFacade.priorityExists(priorityId)) {
            throw new ValidationException("Invalid priorityId");
        }
    }

    private String describe(Letter letter) {
        return (letter.getLetterNumber() != null ? letter.getLetterNumber() : "")
                + " — " + letter.getSubject();
    }

    private List<UUID> nonEmptyIds(List<UUID> ids) {
        return ids == null || ids.isEmpty() ? List.of(new UUID(0L, 0L)) : ids;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
