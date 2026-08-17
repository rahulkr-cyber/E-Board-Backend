package com.bor.eboard.charge.mapper;

import com.bor.eboard.charge.dto.ChargeResponse;
import com.bor.eboard.charge.dto.JoiningRelievingResponse;
import com.bor.eboard.charge.dto.TransferResponse;
import com.bor.eboard.charge.entity.ChargeAssignment;
import com.bor.eboard.charge.entity.JoiningRelieving;
import com.bor.eboard.charge.entity.TransferHistory;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Assembles charge/transfer/posting responses, resolving user, section,
 * department and designation names via the IdentityFacade.
 */
@Component
@RequiredArgsConstructor
public class ChargeMapper {

    public static final String ATTACHMENT_ENTITY_TYPE = "POSTING_ORDER";

    private final IdentityFacade identityFacade;
    private final AttachmentStorageService attachmentStorageService;

    public ChargeResponse toChargeResponse(ChargeAssignment charge) {
        return toChargeResponse(charge, identityFacade.userNames(),
                identityFacade.sectionNames(), identityFacade.departmentNames());
    }

    public ChargeResponse toChargeResponse(ChargeAssignment charge,
                                           Map<UUID, String> users,
                                           Map<UUID, String> sections,
                                           Map<UUID, String> departments) {
        Optional<AttachmentStorageService.StoredAttachment> document =
                document(charge.getAttachmentId(), charge.getId());
        return ChargeResponse.builder()
                .id(charge.getId())
                .fromUserId(charge.getFromUserId())
                .fromUserName(users.get(charge.getFromUserId()))
                .toUserId(charge.getToUserId())
                .toUserName(users.get(charge.getToUserId()))
                .chargeType(charge.getChargeType())
                .departmentId(charge.getDepartmentId())
                .departmentName(departments.get(charge.getDepartmentId()))
                .sectionId(charge.getSectionId())
                .sectionName(sections.get(charge.getSectionId()))
                .effectiveFrom(charge.getEffectiveFrom())
                .effectiveTo(charge.getEffectiveTo())
                .orderNumber(charge.getOrderNumber())
                .orderDate(charge.getOrderDate())
                .approvedBy(charge.getApprovedBy())
                .approvedByName(users.get(charge.getApprovedBy()))
                .attachmentId(charge.getAttachmentId())
                .orderDocumentName(document.map(
                        AttachmentStorageService.StoredAttachment::originalFileName).orElse(null))
                .orderDocumentSize(document.map(
                        AttachmentStorageService.StoredAttachment::fileSize).orElse(null))
                .orderDocumentMimeType(document.map(
                        AttachmentStorageService.StoredAttachment::mimeType).orElse(null))
                .reason(charge.getReason())
                .status(charge.getStatus())
                .currentlyActive(isCurrentlyActive(charge))
                .createdBy(charge.getCreatedBy())
                .createdByName(users.get(charge.getCreatedBy()))
                .createdAt(charge.getCreatedAt())
                .build();
    }

    public TransferResponse toTransferResponse(TransferHistory transfer) {
        Map<UUID, String> users = identityFacade.userNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        Map<UUID, String> departments = identityFacade.departmentNames();
        Map<UUID, String> designations = identityFacade.designationNames();
        Optional<AttachmentStorageService.StoredAttachment> document =
                document(transfer.getAttachmentId(), transfer.getId());
        return TransferResponse.builder()
                .id(transfer.getId())
                .userId(transfer.getUserId())
                .userName(users.get(transfer.getUserId()))
                .fromDepartmentId(transfer.getFromDepartmentId())
                .fromDepartmentName(departments.get(transfer.getFromDepartmentId()))
                .fromSectionId(transfer.getFromSectionId())
                .fromSectionName(sections.get(transfer.getFromSectionId()))
                .toDepartmentId(transfer.getToDepartmentId())
                .toDepartmentName(departments.get(transfer.getToDepartmentId()))
                .toSectionId(transfer.getToSectionId())
                .toSectionName(sections.get(transfer.getToSectionId()))
                .fromDesignationId(transfer.getFromDesignationId())
                .fromDesignationName(designations.get(transfer.getFromDesignationId()))
                .toDesignationId(transfer.getToDesignationId())
                .toDesignationName(designations.get(transfer.getToDesignationId()))
                .postingType(transfer.getPostingType())
                .transferType(transfer.getTransferType())
                .fromOfficeName(transfer.getFromOfficeName())
                .toOfficeName(transfer.getToOfficeName())
                .seatName(transfer.getSeatName())
                .reportingOfficerId(transfer.getReportingOfficerId())
                .reportingOfficerName(users.get(transfer.getReportingOfficerId()))
                .controllingOfficerId(transfer.getControllingOfficerId())
                .controllingOfficerName(users.get(transfer.getControllingOfficerId()))
                .fromLocation(transfer.getFromLocation())
                .toLocation(transfer.getToLocation())
                .fromDistrict(transfer.getFromDistrict())
                .toDistrict(transfer.getToDistrict())
                .transferDate(transfer.getTransferDate())
                .effectiveToDate(transfer.getEffectiveToDate())
                .orderNumber(transfer.getOrderNumber())
                .orderDate(transfer.getOrderDate())
                .governmentOrderNumber(transfer.getGovernmentOrderNumber())
                .governmentOrderDate(transfer.getGovernmentOrderDate())
                .transferReason(transfer.getTransferReason())
                .remarks(transfer.getRemarks())
                .attachmentId(transfer.getAttachmentId())
                .orderDocumentName(document.map(
                        AttachmentStorageService.StoredAttachment::originalFileName).orElse(null))
                .orderDocumentSize(document.map(
                        AttachmentStorageService.StoredAttachment::fileSize).orElse(null))
                .orderDocumentMimeType(document.map(
                        AttachmentStorageService.StoredAttachment::mimeType).orElse(null))
                .createdBy(transfer.getCreatedBy())
                .createdByName(users.get(transfer.getCreatedBy()))
                .createdAt(transfer.getCreatedAt())
                .build();
    }

    public JoiningRelievingResponse toJoiningRelievingResponse(JoiningRelieving event) {
        Map<UUID, String> users = identityFacade.userNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        Map<UUID, String> departments = identityFacade.departmentNames();
        Map<UUID, String> designations = identityFacade.designationNames();
        Optional<AttachmentStorageService.StoredAttachment> document =
                document(event.getAttachmentId(), event.getId());
        return JoiningRelievingResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .userName(users.get(event.getUserId()))
                .eventType(event.getEventType())
                .departmentId(event.getDepartmentId())
                .departmentName(departments.get(event.getDepartmentId()))
                .sectionId(event.getSectionId())
                .sectionName(sections.get(event.getSectionId()))
                .designationId(event.getDesignationId())
                .designationName(designations.get(event.getDesignationId()))
                .eventDate(event.getEventDate())
                .eventTime(event.getEventTime())
                .orderNumber(event.getOrderNumber())
                .orderDate(event.getOrderDate())
                .governmentOrderNumber(event.getGovernmentOrderNumber())
                .governmentOrderDate(event.getGovernmentOrderDate())
                .remarks(event.getRemarks())
                .attachmentId(event.getAttachmentId())
                .orderDocumentName(document.map(
                        AttachmentStorageService.StoredAttachment::originalFileName).orElse(null))
                .orderDocumentSize(document.map(
                        AttachmentStorageService.StoredAttachment::fileSize).orElse(null))
                .orderDocumentMimeType(document.map(
                        AttachmentStorageService.StoredAttachment::mimeType).orElse(null))
                .createdBy(event.getCreatedBy())
                .createdByName(users.get(event.getCreatedBy()))
                .createdAt(event.getCreatedAt())
                .build();
    }

    private boolean isCurrentlyActive(ChargeAssignment charge) {
        if (!"ACTIVE".equals(charge.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean started = charge.getEffectiveFrom() == null
                || !charge.getEffectiveFrom().isAfter(now);
        boolean notEnded = charge.getEffectiveTo() == null
                || !charge.getEffectiveTo().isBefore(now);
        return started && notEnded;
    }

    private Optional<AttachmentStorageService.StoredAttachment> document(
            UUID attachmentId, UUID recordId) {
        if (attachmentId == null || recordId == null) {
            return Optional.empty();
        }
        return attachmentStorageService.listFor(ATTACHMENT_ENTITY_TYPE, recordId).stream()
                .filter(item -> item.id().equals(attachmentId))
                .findFirst();
    }
}
