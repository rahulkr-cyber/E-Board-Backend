package com.bor.eboard.registry.mapper;

import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.registry.dto.AttachmentResponse;
import com.bor.eboard.registry.dto.DiaryEntryResponse;
import com.bor.eboard.registry.dto.ReceiptResponse;
import com.bor.eboard.registry.entity.DiaryEntry;
import com.bor.eboard.registry.entity.ReceiptRegisterEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Manual mapper: reference names are resolved by the service layer and
 * passed in as lookup maps (no JPA associations anywhere).
 */
@Component
public class DiaryMapper {

    public DiaryEntryResponse toResponse(DiaryEntry entry,
                                         Map<UUID, String> categoryNames,
                                         Map<UUID, String> priorityNames,
                                         Map<UUID, String> languageNames,
                                         Map<UUID, String> departmentNames,
                                         Map<UUID, String> sectionNames,
                                         Map<UUID, String> userNames,
                                         ReceiptRegisterEntry receipt,
                                         List<AttachmentStorageService.StoredAttachment> attachments) {
        Function<UUID, String> user = id -> id != null ? userNames.get(id) : null;
        return DiaryEntryResponse.builder()
                .id(entry.getId())
                .diaryNumber(entry.getDiaryNumber())
                .diaryYear(entry.getDiaryYear())
                .diarySequence(entry.getDiarySequence())
                .sourceType(entry.getSourceType())
                .receivedMode(entry.getReceivedMode())
                .receivedDate(entry.getReceivedDate())
                .receivedBy(entry.getReceivedBy())
                .receivedByName(user.apply(entry.getReceivedBy()))
                .senderName(entry.getSenderName())
                .senderDesignation(entry.getSenderDesignation())
                .senderDepartment(entry.getSenderDepartment())
                .senderAddress(entry.getSenderAddress())
                .senderEmail(entry.getSenderEmail())
                .senderMobile(entry.getSenderMobile())
                .originalLetterNumber(entry.getOriginalLetterNumber())
                .referenceNumber(entry.getReferenceNumber())
                .letterDate(entry.getLetterDate())
                .subject(entry.getSubject())
                .description(entry.getDescription())
                .categoryId(entry.getCategoryId())
                .categoryName(entry.getCategoryId() != null
                        ? categoryNames.get(entry.getCategoryId()) : null)
                .priorityId(entry.getPriorityId())
                .priorityName(entry.getPriorityId() != null
                        ? priorityNames.get(entry.getPriorityId()) : null)
                .languageId(entry.getLanguageId())
                .languageName(entry.getLanguageId() != null
                        ? languageNames.get(entry.getLanguageId()) : null)
                .confidential(entry.getConfidential())
                .dueDate(entry.getDueDate())
                .reminderDate(entry.getReminderDate())
                .pageCount(entry.getPageCount())
                .physicalCopyReceived(entry.getPhysicalCopyReceived())
                .barcodeValue(entry.getBarcodeValue())
                .qrCodeValue(entry.getQrCodeValue())
                .initialDepartmentId(entry.getInitialDepartmentId())
                .initialDepartmentName(entry.getInitialDepartmentId() != null
                        ? departmentNames.get(entry.getInitialDepartmentId()) : null)
                .initialSectionId(entry.getInitialSectionId())
                .initialSectionName(entry.getInitialSectionId() != null
                        ? sectionNames.get(entry.getInitialSectionId()) : null)
                .initialAssignedUserId(entry.getInitialAssignedUserId())
                .initialAssignedUserName(user.apply(entry.getInitialAssignedUserId()))
                .status(entry.getStatus())
                .createdAt(entry.getCreatedAt())
                .receipt(receipt != null ? toReceipt(receipt, userNames) : null)
                .attachments(attachments != null
                        ? attachments.stream().map(this::toAttachment).toList()
                        : List.of())
                .build();
    }

    public ReceiptResponse toReceipt(ReceiptRegisterEntry receipt, Map<UUID, String> userNames) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .receivedFrom(receipt.getReceivedFrom())
                .receivedBy(receipt.getReceivedBy())
                .receivedByName(userNames.get(receipt.getReceivedBy()))
                .receivedAt(receipt.getReceivedAt())
                .remarks(receipt.getRemarks())
                .build();
    }

    public AttachmentResponse toAttachment(AttachmentStorageService.StoredAttachment stored) {
        return AttachmentResponse.builder()
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
