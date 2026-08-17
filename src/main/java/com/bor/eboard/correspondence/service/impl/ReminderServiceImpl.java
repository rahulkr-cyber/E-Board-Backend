package com.bor.eboard.correspondence.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.core.service.NumberGenerationService;
import com.bor.eboard.correspondence.dto.CreateReminderRequest;
import com.bor.eboard.correspondence.dto.ReminderResponse;
import com.bor.eboard.correspondence.dto.UpdateReminderStatusRequest;
import com.bor.eboard.correspondence.entity.FileEntity;
import com.bor.eboard.correspondence.entity.Reminder;
import com.bor.eboard.correspondence.mapper.CorrespondenceLookups;
import com.bor.eboard.correspondence.mapper.CorrespondenceMapper;
import com.bor.eboard.correspondence.mapper.LookupProvider;
import com.bor.eboard.correspondence.repository.FileRepository;
import com.bor.eboard.correspondence.repository.ReminderRepository;
import com.bor.eboard.correspondence.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private static final String MODULE = "CORRESPONDENCE";
    private static final Set<String> LOCKED = Set.of("CLOSED", "ARCHIVED");

    private final ReminderRepository reminderRepository;
    private final FileRepository fileRepository;
    private final NumberGenerationService numberGenerationService;
    private final CorrespondenceMapper mapper;
    private final LookupProvider lookupProvider;
    private final AuditService auditService;

    @Override
    @Transactional
    public ReminderResponse create(UUID fileId, CreateReminderRequest request) {
        FileEntity file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
        if (LOCKED.contains(file.getCurrentStatus())) {
            throw new BusinessException(
                    "Reminders cannot be added to a " + file.getCurrentStatus() + " file");
        }
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);

        NumberGenerationService.GeneratedNumber reminderNumber =
                numberGenerationService.next("REMINDER", "BOR/REM");

        Reminder reminder = new Reminder();
        reminder.setFileId(fileId);
        reminder.setLetterId(request.getLetterId());
        reminder.setReminderNumber(reminderNumber.formatted());
        reminder.setReminderDate(request.getReminderDate());
        reminder.setReminderType(request.getReminderType());
        reminder.setRemarks(request.getRemarks());
        reminder.setGeneratedBy(currentUserId);
        reminder.setStatus("DRAFT");
        reminder = reminderRepository.save(reminder);

        auditService.record(MODULE, "REMINDER", reminder.getId(), "CREATE", null,
                "reminderNumber=" + reminder.getReminderNumber() + ", fileId=" + fileId);

        CorrespondenceLookups lookups = lookupProvider.load();
        return mapper.toReminderResponse(reminder, lookups);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderResponse> listByFile(UUID fileId) {
        CorrespondenceLookups lookups = lookupProvider.load();
        return reminderRepository.findByFileIdAndDeletedFalseOrderByReminderDateDesc(fileId).stream()
                .map(reminder -> mapper.toReminderResponse(reminder, lookups)).toList();
    }

    @Override
    @Transactional
    public ReminderResponse updateStatus(UUID id, UpdateReminderStatusRequest request) {
        Reminder reminder = reminderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", id));
        String oldStatus = reminder.getStatus();
        reminder.setStatus(request.getStatus());
        if (request.getRemarks() != null) {
            reminder.setRemarks(request.getRemarks());
        }
        reminder = reminderRepository.save(reminder);
        auditService.record(MODULE, "REMINDER", reminder.getId(), "UPDATE_STATUS",
                oldStatus, reminder.getStatus());
        CorrespondenceLookups lookups = lookupProvider.load();
        return mapper.toReminderResponse(reminder, lookups);
    }
}
