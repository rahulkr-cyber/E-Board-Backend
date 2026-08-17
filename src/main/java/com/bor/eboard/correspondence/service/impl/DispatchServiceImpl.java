package com.bor.eboard.correspondence.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.correspondence.dto.CreateFollowupRequest;
import com.bor.eboard.correspondence.dto.DispatchTargetResponse;
import com.bor.eboard.correspondence.dto.FollowupResponse;
import com.bor.eboard.correspondence.dto.UpdateFollowupRequest;
import com.bor.eboard.correspondence.entity.Followup;
import com.bor.eboard.correspondence.repository.FollowupRepository;
import com.bor.eboard.core.service.NumberGenerationService;
import com.bor.eboard.correspondence.dto.CreateDispatchRequest;
import com.bor.eboard.correspondence.dto.DispatchRegisterRow;
import com.bor.eboard.correspondence.dto.DispatchResponse;
import com.bor.eboard.correspondence.dto.UpdateDispatchStatusRequest;
import com.bor.eboard.correspondence.entity.DispatchRegisterEntry;
import com.bor.eboard.correspondence.entity.FileEntity;
import com.bor.eboard.correspondence.entity.Letter;
import com.bor.eboard.correspondence.mapper.CorrespondenceMapper;
import com.bor.eboard.correspondence.repository.DispatchRepository;
import com.bor.eboard.correspondence.repository.FileRepository;
import com.bor.eboard.correspondence.repository.LetterRepository;
import com.bor.eboard.correspondence.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Dispatch implementation.
 *
 * Enforced rules:
 * - Dispatch number BOR/DISPATCH/{YEAR}/{SEQ}: system-generated, immutable.
 * - Only OUTWARD letters can be dispatched.
 * - New dispatch starts PENDING.
 */
@Service
@RequiredArgsConstructor
public class DispatchServiceImpl implements DispatchService {

    private static final String MODULE = "CORRESPONDENCE";
    private static final String STATUS_PENDING = "PENDING";
    private static final Set<String> TERMINAL = Set.of("DELIVERED", "CANCELLED");

    private final DispatchRepository dispatchRepository;
    private final LetterRepository letterRepository;
    private final FileRepository fileRepository;
    private final NumberGenerationService numberGenerationService;
    private final CorrespondenceMapper mapper;
    private final AuditService auditService;
    // BCR-03 Parts 9-10
    private final FollowupRepository followupRepository;
    private final com.bor.eboard.identity.facade.IdentityFacade identityFacade;
    private final com.bor.eboard.admin.facade.MasterDataFacade masterDataFacade;

    @Override
    @Transactional
    public DispatchResponse create(CreateDispatchRequest request) {
        // BCR-03 Part 9: a dispatch is raised against a Letter OR a File.
        if (request.getLetterId() == null && request.getFileId() == null) {
            throw new ValidationException("Select a letter or a file to dispatch against.");
        }
        if (request.getLetterId() != null && request.getFileId() != null) {
            throw new ValidationException("Select either a letter or a file, not both.");
        }
        Letter letter = request.getLetterId() == null ? null
                : letterRepository.findByIdAndDeletedFalse(request.getLetterId())
                        .orElseThrow(() -> new ValidationException("Invalid letter reference"));
        FileEntity targetFile = request.getFileId() == null ? null
                : fileRepository.findByIdAndDeletedFalse(request.getFileId())
                        .orElseThrow(() -> new ValidationException("Invalid file reference"));
        if (letter != null && !"OUTWARD".equals(letter.getLetterDirection())) {
            throw new BusinessException("Only OUTWARD letters can be dispatched");
        }

        NumberGenerationService.GeneratedNumber dispatchNumber =
                numberGenerationService.next("DISPATCH", "BOR/DISPATCH");

        DispatchRegisterEntry dispatch = new DispatchRegisterEntry();
        // Exactly one of these is non-null (validated above).
        dispatch.setLetterId(letter != null ? letter.getId() : null);
        dispatch.setFileId(targetFile != null ? targetFile.getId() : null);
        dispatch.setDispatchNumber(dispatchNumber.formatted());
        dispatch.setDispatchDate(request.getDispatchDate());
        dispatch.setDispatchMode(blankToNull(request.getDispatchMode()));
        dispatch.setRecipientName(request.getRecipientName());
        dispatch.setRecipientDepartment(request.getRecipientDepartment());
        dispatch.setRecipientAddress(request.getRecipientAddress());
        dispatch.setTrackingNumber(request.getTrackingNumber());
        dispatch.setStatus(STATUS_PENDING);
        dispatch.setRemarks(request.getRemarks());
        dispatch = dispatchRepository.save(dispatch);

        auditService.record(MODULE, "DISPATCH", dispatch.getId(), "CREATE", null,
                "dispatchNumber=" + dispatch.getDispatchNumber()
                        + (letter != null ? ", letterId=" + letter.getId()
                                          : ", fileId=" + targetFile.getId()));

        return mapper.toDispatchResponse(dispatch, letter,
                letter != null ? fileNumberFor(letter) : targetFile.getFileNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchResponse getById(UUID id) {
        DispatchRegisterEntry dispatch = findDispatch(id);
        Letter letter = letterRepository.findByIdAndDeletedFalse(dispatch.getLetterId()).orElse(null);
        return mapper.toDispatchResponse(dispatch, letter, fileNumberFor(letter));
    }

    @Override
    @Transactional
    public DispatchResponse updateStatus(UUID id, UpdateDispatchStatusRequest request) {
        DispatchRegisterEntry dispatch = findDispatch(id);
        if (TERMINAL.contains(dispatch.getStatus())) {
            throw new BusinessException(
                    "Dispatch is " + dispatch.getStatus() + " and can no longer be updated");
        }
        String oldStatus = dispatch.getStatus();
        dispatch.setStatus(request.getStatus());
        if (request.getTrackingNumber() != null) {
            dispatch.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getRemarks() != null) {
            dispatch.setRemarks(request.getRemarks());
        }
        dispatch = dispatchRepository.save(dispatch);
        auditService.record(MODULE, "DISPATCH", dispatch.getId(), "UPDATE_STATUS",
                oldStatus, request.getStatus());
        Letter letter = letterRepository.findByIdAndDeletedFalse(dispatch.getLetterId()).orElse(null);
        return mapper.toDispatchResponse(dispatch, letter, fileNumberFor(letter));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DispatchResponse> search(
            String dispatchNumber,
            String recipient,
            String mode,
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            PaginationRequest pagination) {

        String normalizedDispatchNumber = StringUtils.hasText(dispatchNumber)
                ? "%" + dispatchNumber.trim().toLowerCase(Locale.ROOT) + "%"
                : null;

        String normalizedRecipient = StringUtils.hasText(recipient)
                ? "%" + recipient.trim().toLowerCase(Locale.ROOT) + "%"
                : null;

        String normalizedMode = StringUtils.hasText(mode)
                ? mode.trim()
                : null;

        String normalizedStatus = StringUtils.hasText(status)
                ? status.trim()
                : null;

        LocalDate normalizedFromDate = fromDate != null
                ? fromDate
                : LocalDate.of(1900, 1, 1);

        LocalDate normalizedToDate = toDate != null
                ? toDate
                : LocalDate.of(9999, 12, 31);

        Page<DispatchRegisterEntry> page = dispatchRepository.search(
                normalizedDispatchNumber,
                normalizedRecipient,
                normalizedMode,
                normalizedStatus,
                normalizedFromDate,
                normalizedToDate,
                pagination.toPageable());

        Map<UUID, Letter> letterCache = new HashMap<>();

        List<DispatchResponse> content = page.getContent()
                .stream()
                .map(dispatch -> {
                    Letter letter = letterCache.computeIfAbsent(
                            dispatch.getLetterId(),
                            lid -> letterRepository.findByIdAndDeletedFalse(lid).orElse(null));

                    return mapper.toDispatchResponse(
                            dispatch,
                            letter,
                            fileNumberFor(letter));
                })
                .toList();

        return PageResponse.of(content, page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchRegisterRow> dispatchRegister(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new ValidationException("fromDate and toDate are required");
        }
        if (toDate.isBefore(fromDate)) {
            throw new ValidationException("toDate cannot be before fromDate");
        }
        Map<UUID, Letter> letterCache = new HashMap<>();
        return dispatchRepository.dispatchRegister(fromDate, toDate).stream()
                .map(dispatch -> {
                    Letter letter = letterCache.computeIfAbsent(dispatch.getLetterId(),
                            lid -> letterRepository.findByIdAndDeletedFalse(lid).orElse(null));
                    return DispatchRegisterRow.builder()
                            .id(dispatch.getId())
                            .dispatchNumber(dispatch.getDispatchNumber())
                            .dispatchDate(dispatch.getDispatchDate())
                            .letterNumber(letter != null ? letter.getLetterNumber() : null)
                            .fileNumber(fileNumberFor(letter))
                            .recipientName(dispatch.getRecipientName())
                            .recipientDepartment(dispatch.getRecipientDepartment())
                            .dispatchMode(dispatch.getDispatchMode())
                            .trackingNumber(dispatch.getTrackingNumber())
                            .status(dispatch.getStatus())
                            .build();
                })
                .toList();
    }

    // ------------------------------------------------------------------

    private DispatchRegisterEntry findDispatch(UUID id) {
        return dispatchRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch", id));
    }

    private String fileNumberFor(Letter letter) {
        if (letter == null || letter.getFileId() == null) {
            return null;
        }
        return fileRepository.findByIdAndDeletedFalse(letter.getFileId())
                .map(FileEntity::getFileNumber).orElse(null);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // =====================================================================
    // BCR-03 Part 9: searchable dispatch target. The clerk searches by number
    // or subject and picks a row; context auto-populates. No UUID is typed.
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<DispatchTargetResponse> searchTargets(String query) {
        if (query == null || query.trim().length() < 2) {
            throw new ValidationException("Enter at least 2 characters to search.");
        }
        String q = query.trim();
        Pageable limit = PageRequest.of(0, 10);

        Map<UUID, String> sections = identityFacade.sectionNames();
        Map<UUID, String> users = identityFacade.userNames();
        Map<UUID, String> categories = masterDataFacade.categoryNames();

        List<DispatchTargetResponse> results = new ArrayList<>();

        for (FileEntity f : fileRepository.searchForDispatchTarget(q, limit)) {
            results.add(DispatchTargetResponse.builder()
                    .targetType("FILE")
                    .targetId(f.getId())
                    .referenceNumber(f.getFileNumber())
                    .subject(f.getSubject())
                    .sectionId(f.getCurrentSectionId())
                    .sectionName(f.getCurrentSectionId() == null
                            ? null : sections.get(f.getCurrentSectionId()))
                    .currentOwnerId(f.getCurrentOwnerId())
                    .currentOwnerName(f.getCurrentOwnerId() == null
                            ? null : users.get(f.getCurrentOwnerId()))
                    .categoryId(f.getCategoryId())
                    .categoryName(f.getCategoryId() == null
                            ? null : categories.get(f.getCategoryId()))
                    .build());
        }

        for (Letter l : letterRepository.searchForDispatchTarget(q, limit)) {
            results.add(DispatchTargetResponse.builder()
                    .targetType("LETTER")
                    .targetId(l.getId())
                    .referenceNumber(l.getLetterNumber() != null
                            ? l.getLetterNumber() : l.getReferenceNumber())
                    .subject(l.getSubject())
                    .sectionId(l.getCurrentSectionId())
                    .sectionName(l.getCurrentSectionId() == null
                            ? null : sections.get(l.getCurrentSectionId()))
                    .currentOwnerId(l.getCurrentOwnerId())
                    .currentOwnerName(l.getCurrentOwnerId() == null
                            ? null : users.get(l.getCurrentOwnerId()))
                    .categoryId(l.getCategoryId())
                    .categoryName(l.getCategoryId() == null
                            ? null : categories.get(l.getCategoryId()))
                    .build());
        }
        return results;
    }

    // =====================================================================
    // BCR-03 Part 10: follow-ups on a dispatch, with automatic reminders.
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<FollowupResponse> followups(UUID dispatchId) {
        return followupRepository
                .findByDispatchIdAndDeletedFalseOrderByFollowupDateAsc(dispatchId).stream()
                .map(this::toFollowupResponse)
                .toList();
    }

    @Override
    @Transactional
    public FollowupResponse addFollowup(UUID dispatchId, CreateFollowupRequest request) {
        DispatchRegisterEntry dispatch = dispatchRepository.findByIdAndDeletedFalse(dispatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch", dispatchId));

        if (request.getReminderDate() != null
                && request.getReminderDate().isAfter(request.getDueDate())) {
            throw new ValidationException("The reminder date cannot be after the due date.");
        }

        int sequence = followupRepository
                .findByDispatchIdAndDeletedFalseOrderByFollowupDateAsc(dispatchId).size() + 1;

        Followup followup = new Followup();
        followup.setDispatchId(dispatchId);
        followup.setLetterId(dispatch.getLetterId());
        followup.setFileId(dispatch.getFileId());
        followup.setFollowupNumber(dispatch.getDispatchNumber() + "/F" + sequence);
        followup.setFollowupDate(LocalDate.now());
        followup.setFollowupType("DISPATCH");
        followup.setDueDate(request.getDueDate());
        followup.setReminderDate(request.getReminderDate());
        followup.setReplyReceived(Boolean.FALSE);
        followup.setStatus("PENDING");
        followup.setRemarks(request.getRemarks());
        followup = followupRepository.save(followup);

        auditService.record(MODULE, "FOLLOWUP", followup.getId(), "CREATE", null,
                "Follow-up " + followup.getFollowupNumber()
                        + " due " + followup.getDueDate());

        return toFollowupResponse(followup);
    }

    @Override
    @Transactional
    public FollowupResponse updateFollowup(UUID followupId, UpdateFollowupRequest request) {
        Followup followup = followupRepository.findById(followupId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up", followupId));

        String previousStatus = followup.getStatus();

        if (Boolean.TRUE.equals(request.getReplyReceived())) {
            followup.setReplyReceived(Boolean.TRUE);
            followup.setReplyReceivedDate(request.getReplyReceivedDate() != null
                    ? request.getReplyReceivedDate() : LocalDate.now());
            followup.setStatus("CLOSED");
        }
        if (request.getDueDate() != null) {
            followup.setDueDate(request.getDueDate());
        }
        if (request.getReminderDate() != null) {
            followup.setReminderDate(request.getReminderDate());
        }
        if (request.getRemarks() != null) {
            followup.setRemarks(request.getRemarks());
        }
        followup = followupRepository.save(followup);

        auditService.record(MODULE, "FOLLOWUP", followup.getId(), "UPDATE",
                previousStatus, followup.getStatus());

        return toFollowupResponse(followup);
    }

    private FollowupResponse toFollowupResponse(Followup f) {
        long pendingDays = 0L;
        if (!Boolean.TRUE.equals(f.getReplyReceived()) && f.getDueDate() != null
                && f.getDueDate().isBefore(LocalDate.now())) {
            pendingDays = java.time.temporal.ChronoUnit.DAYS
                    .between(f.getDueDate(), LocalDate.now());
        }
        return FollowupResponse.builder()
                .id(f.getId())
                .dispatchId(f.getDispatchId())
                .followupNumber(f.getFollowupNumber())
                .followupDate(f.getFollowupDate())
                .dueDate(f.getDueDate())
                .reminderDate(f.getReminderDate())
                .replyReceived(f.getReplyReceived())
                .replyReceivedDate(f.getReplyReceivedDate())
                .status(f.getStatus())
                .remarks(f.getRemarks())
                .pendingDays(pendingDays)
                .build();
    }
}
