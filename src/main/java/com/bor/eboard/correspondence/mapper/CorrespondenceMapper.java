package com.bor.eboard.correspondence.mapper;

import com.bor.eboard.correspondence.dto.AttachmentInfo;
import com.bor.eboard.correspondence.dto.DispatchResponse;
import com.bor.eboard.correspondence.dto.FileResponse;
import com.bor.eboard.correspondence.dto.FollowupResponse;
import com.bor.eboard.correspondence.dto.LetterResponse;
import com.bor.eboard.correspondence.dto.ReminderResponse;
import com.bor.eboard.correspondence.entity.DispatchRegisterEntry;
import com.bor.eboard.correspondence.entity.FileEntity;
import com.bor.eboard.correspondence.entity.Followup;
import com.bor.eboard.correspondence.entity.Letter;
import com.bor.eboard.correspondence.entity.Reminder;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manual mappers for the Correspondence module. Reference names come from
 * the shared lookup bundle; there are no entity associations anywhere.
 */
@Component
public class CorrespondenceMapper {

    public FileResponse toFileResponse(FileEntity file,
                                       CorrespondenceLookups lookups,
                                       List<LetterResponse> letters,
                                       List<AttachmentInfo> attachments) {
        return FileResponse.builder()
                .id(file.getId())
                .fileNumber(file.getFileNumber())
                .fileYear(file.getFileYear())
                .fileSequence(file.getFileSequence())
                .subject(file.getSubject())
                .description(file.getDescription())
                .categoryId(file.getCategoryId())
                .categoryName(lookups.category(file.getCategoryId()))
                .priorityId(file.getPriorityId())
                .priorityName(lookups.priority(file.getPriorityId()))
                .departmentId(file.getDepartmentId())
                .departmentName(lookups.department(file.getDepartmentId()))
                .sectionId(file.getSectionId())
                .sectionName(lookups.section(file.getSectionId()))
                .currentOwnerId(file.getCurrentOwnerId())
                .currentOwnerName(lookups.user(file.getCurrentOwnerId()))
                .currentSectionId(file.getCurrentSectionId())
                .currentSectionName(lookups.section(file.getCurrentSectionId()))
                .currentStatus(file.getCurrentStatus())
                .openedDate(file.getOpenedDate())
                .closedDate(file.getClosedDate())
                .archivedDate(file.getArchivedDate())
                .confidential(file.getConfidential())
                .createdAt(file.getCreatedAt())
                .letters(letters)
                .attachments(attachments)
                .build();
    }

    public LetterResponse toLetterResponse(Letter letter,
                                           String fileNumber,
                                           CorrespondenceLookups lookups,
                                           List<AttachmentInfo> attachments) {
        return LetterResponse.builder()
                .id(letter.getId())
                .fileId(letter.getFileId())
                .fileNumber(fileNumber)
                .diaryEntryId(letter.getDiaryEntryId())
                .letterDirection(letter.getLetterDirection())
                .letterType(letter.getLetterType())
                .letterNumber(letter.getLetterNumber())
                .referenceNumber(letter.getReferenceNumber())
                .letterDate(letter.getLetterDate())
                .subject(letter.getSubject())
                .body(letter.getBody())
                .senderName(letter.getSenderName())
                .senderDesignation(letter.getSenderDesignation())
                .senderDepartment(letter.getSenderDepartment())
                .senderAddress(letter.getSenderAddress())
                .receiverDepartmentId(letter.getReceiverDepartmentId())
                .receiverDepartmentName(lookups.department(letter.getReceiverDepartmentId()))
                .receiverSectionId(letter.getReceiverSectionId())
                .receiverSectionName(lookups.section(letter.getReceiverSectionId()))
                .receiverUserId(letter.getReceiverUserId())
                .receiverUserName(lookups.user(letter.getReceiverUserId()))
                .categoryId(letter.getCategoryId())
                .categoryName(lookups.category(letter.getCategoryId()))
                .priorityId(letter.getPriorityId())
                .priorityName(lookups.priority(letter.getPriorityId()))
                .languageId(letter.getLanguageId())
                .languageName(lookups.language(letter.getLanguageId()))
                .confidential(letter.getConfidential())
                .dueDate(letter.getDueDate())
                .reminderDate(letter.getReminderDate())
                .currentOwnerId(letter.getCurrentOwnerId())
                .currentSectionId(letter.getCurrentSectionId())
                .currentStatus(letter.getCurrentStatus())
                .createdAt(letter.getCreatedAt())
                .attachments(attachments)
                .build();
    }

    public DispatchResponse toDispatchResponse(DispatchRegisterEntry dispatch,
                                               Letter letter,
                                               String fileNumber) {
        return DispatchResponse.builder()
                .id(dispatch.getId())
                .letterId(dispatch.getLetterId())
                .letterNumber(letter != null ? letter.getLetterNumber() : null)
                .letterSubject(letter != null ? letter.getSubject() : null)
                .fileId(letter != null ? letter.getFileId() : null)
                .fileNumber(fileNumber)
                .dispatchNumber(dispatch.getDispatchNumber())
                .dispatchDate(dispatch.getDispatchDate())
                .dispatchMode(dispatch.getDispatchMode())
                .recipientName(dispatch.getRecipientName())
                .recipientDepartment(dispatch.getRecipientDepartment())
                .recipientAddress(dispatch.getRecipientAddress())
                .trackingNumber(dispatch.getTrackingNumber())
                .status(dispatch.getStatus())
                .remarks(dispatch.getRemarks())
                .createdAt(dispatch.getCreatedAt())
                .build();
    }

    public FollowupResponse toFollowupResponse(Followup followup) {
        return FollowupResponse.builder()
                .id(followup.getId())
                .fileId(followup.getFileId())
                .letterId(followup.getLetterId())
                .followupDate(followup.getFollowupDate())
                .dueDate(followup.getDueDate())
                .reminderDate(followup.getReminderDate())
                .followupType(followup.getFollowupType())
                .remarks(followup.getRemarks())
                .nextFollowupDate(followup.getNextFollowupDate())
                .replyReceived(followup.getReplyReceived())
                .replyReceivedDate(followup.getReplyReceivedDate())
                .status(followup.getStatus())
                .createdAt(followup.getCreatedAt())
                .build();
    }

    public ReminderResponse toReminderResponse(Reminder reminder,
                                               CorrespondenceLookups lookups) {
        return ReminderResponse.builder()
                .id(reminder.getId())
                .fileId(reminder.getFileId())
                .letterId(reminder.getLetterId())
                .reminderNumber(reminder.getReminderNumber())
                .reminderDate(reminder.getReminderDate())
                .reminderType(reminder.getReminderType())
                .remarks(reminder.getRemarks())
                .generatedBy(reminder.getGeneratedBy())
                .generatedByName(lookups.user(reminder.getGeneratedBy()))
                .status(reminder.getStatus())
                .createdAt(reminder.getCreatedAt())
                .build();
    }

    public AttachmentInfo toAttachmentInfo(AttachmentStorageService.StoredAttachment stored) {
        return AttachmentInfo.builder()
                .id(stored.id())
                .originalFileName(stored.originalFileName())
                .fileExtension(stored.fileExtension())
                .mimeType(stored.mimeType())
                .fileSize(stored.fileSize())
                .checksum(stored.checksum())
                .uploadedAt(stored.uploadedAt())
                .downloadUrl("/api/v1/files/storage/" + stored.id() + "/download")
                .build();
    }
}
