package com.bor.eboard.correspondence.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.correspondence.dto.CreateFollowupRequest;
import com.bor.eboard.correspondence.dto.FollowupResponse;
import com.bor.eboard.correspondence.dto.UpdateFollowupRequest;
import com.bor.eboard.correspondence.entity.FileEntity;
import com.bor.eboard.correspondence.entity.Followup;
import com.bor.eboard.correspondence.mapper.CorrespondenceMapper;
import com.bor.eboard.correspondence.repository.FileRepository;
import com.bor.eboard.correspondence.repository.FollowupRepository;
import com.bor.eboard.correspondence.service.FollowupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowupServiceImpl implements FollowupService {

    private static final String MODULE = "CORRESPONDENCE";
    private static final Set<String> LOCKED = Set.of("CLOSED", "ARCHIVED");

    private final FollowupRepository followupRepository;
    private final FileRepository fileRepository;
    private final com.bor.eboard.correspondence.repository.LetterRepository letterRepository;
    private final CorrespondenceMapper mapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public FollowupResponse create(UUID fileId, CreateFollowupRequest request) {

        FileEntity file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        if (LOCKED.contains(file.getCurrentStatus())) {
            throw new BusinessException(
                    "Follow-ups cannot be added to a " + file.getCurrentStatus() + " file");
        }

        Followup followup = new Followup();
        followup.setFileId(fileId);
        followup.setLetterId(request.getLetterId());

        // Follow-up is raised today
        followup.setFollowupDate(java.time.LocalDate.now());

        followup.setFollowupType(request.getFollowupType());
        followup.setRemarks(request.getRemarks());

        // New fields
        followup.setDueDate(request.getDueDate());
        followup.setReminderDate(request.getReminderDate());

        // Optional
//        followup.setNextFollowupDate(request.getReminderDate());
        
     
        followup.setNextFollowupDate(request.getNextFollowupDate());

        followup.setReplyReceived(false);
        followup.setStatus("OPEN");

        followup = followupRepository.save(followup);

        auditService.record(
                MODULE,
                "FOLLOWUP",
                followup.getId(),
                "CREATE",
                null,
                "fileId=" + fileId + ", dueDate=" + followup.getDueDate());

        return mapper.toFollowupResponse(followup);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowupResponse> listByFile(UUID fileId) {
        return followupRepository.findByFileIdAndDeletedFalseOrderByFollowupDateDesc(fileId).stream()
                .map(mapper::toFollowupResponse).toList();
    }

    @Override
    @Transactional
    public FollowupResponse createForLetter(UUID letterId, CreateFollowupRequest request) {
        var letter = letterRepository.findByIdAndDeletedFalse(letterId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter", letterId));
        if (LOCKED.contains(letter.getCurrentStatus())) {
            throw new BusinessException("Follow-ups cannot be added to a " + letter.getCurrentStatus() + " letter");
        }
        Followup followup = new Followup();
        followup.setLetterId(letterId);
        followup.setFollowupDate(java.time.LocalDate.now());
        followup.setFollowupType(request.getFollowupType());
        followup.setRemarks(request.getRemarks());
        followup.setDueDate(request.getDueDate());
        followup.setReminderDate(request.getReminderDate());
        followup.setNextFollowupDate(request.getReminderDate());
        followup.setReplyReceived(false);
        followup.setStatus("OPEN");
        followup = followupRepository.save(followup);
        auditService.record(MODULE, "FOLLOWUP", followup.getId(), "CREATE", null,
                "letterId=" + letterId + ", dueDate=" + followup.getDueDate());
        return mapper.toFollowupResponse(followup);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowupResponse> listByLetter(UUID letterId) {
        if (!letterRepository.findByIdAndDeletedFalse(letterId).isPresent()) {
            throw new ResourceNotFoundException("Letter", letterId);
        }
        return followupRepository.findByLetterIdAndDeletedFalseOrderByFollowupDateDesc(letterId).stream()
                .map(mapper::toFollowupResponse).toList();
    }

    @Override
    @Transactional
    public FollowupResponse update(UUID id, UpdateFollowupRequest request) {

        Followup followup = followupRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up", id));

        String oldStatus = followup.getStatus();

        // Status
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            followup.setStatus(request.getStatus().trim().toUpperCase());
        }

        // Remarks
        if (request.getRemarks() != null) {
            followup.setRemarks(request.getRemarks());
        }

        // Due Date
        if (request.getDueDate() != null) {
            followup.setDueDate(request.getDueDate());
        }

        // Reminder Date
        if (request.getReminderDate() != null) {
            followup.setReminderDate(request.getReminderDate());
        }

        // Next Follow-up Date
        if (request.getNextFollowupDate() != null) {
            followup.setNextFollowupDate(request.getNextFollowupDate());
        }

        // Reply Received
        if (request.getReplyReceived() != null) {

            followup.setReplyReceived(request.getReplyReceived());

            if (Boolean.TRUE.equals(request.getReplyReceived())) {

                followup.setReplyReceivedDate(
                        request.getReplyReceivedDate() != null
                                ? request.getReplyReceivedDate()
                                : java.time.LocalDate.now());

                // Automatically close the follow-up if reply is received
                if (followup.getStatus() == null
                        || !"CLOSED".equalsIgnoreCase(followup.getStatus())) {
                    followup.setStatus("CLOSED");
                }

            } else {
                followup.setReplyReceivedDate(null);
            }
        }

        // ===========================
        // Business Validations
        // ===========================

        if (followup.getDueDate() != null
                && followup.getReminderDate() != null
                && followup.getReminderDate().isAfter(followup.getDueDate())) {
            throw new ValidationException(
                    "Reminder date cannot be after due date.");
        }

        if (followup.getFollowupDate() != null
                && followup.getNextFollowupDate() != null
                && followup.getNextFollowupDate().isBefore(followup.getFollowupDate())) {
            throw new ValidationException(
                    "Next follow-up date cannot be before follow-up date.");
        }

        if (Boolean.TRUE.equals(followup.getReplyReceived())
                && followup.getReplyReceivedDate() == null) {
            throw new ValidationException(
                    "Reply received date is required when reply is received.");
        }

        if (followup.getReplyReceivedDate() != null
                && followup.getFollowupDate() != null
                && followup.getReplyReceivedDate().isBefore(followup.getFollowupDate())) {
            throw new ValidationException(
                    "Reply received date cannot be before follow-up date.");
        }

        followup = followupRepository.save(followup);

        auditService.record(
                MODULE,
                "FOLLOWUP",
                followup.getId(),
                "UPDATE",
                oldStatus,
                "status=" + followup.getStatus()
                        + ", dueDate=" + followup.getDueDate()
                        + ", reminderDate=" + followup.getReminderDate()
                        + ", nextFollowupDate=" + followup.getNextFollowupDate()
                        + ", replyReceived=" + followup.getReplyReceived());

        return mapper.toFollowupResponse(followup);
    }
}
