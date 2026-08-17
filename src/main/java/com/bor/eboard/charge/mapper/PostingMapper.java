package com.bor.eboard.charge.mapper;

import com.bor.eboard.charge.dto.ChargeResponse;
import com.bor.eboard.charge.dto.JoiningRelievingResponse;
import com.bor.eboard.charge.dto.PostingDocumentResponse;
import com.bor.eboard.charge.dto.PostingTimelineEntryResponse;
import com.bor.eboard.charge.dto.TransferResponse;
import com.bor.eboard.charge.entity.UserPosting;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostingMapper {

    public static final String ORDER_ENTITY_TYPE = "POSTING_ORDER";
    public static final String SUPPORTING_ENTITY_TYPE = "POSTING_SUPPORTING";

    private final IdentityFacade identityFacade;
    private final AttachmentStorageService attachmentStorageService;

    public PostingTimelineEntryResponse toResponse(UserPosting posting) {
        Map<UUID, String> departments = identityFacade.departmentNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        Map<UUID, String> designations = identityFacade.designationNames();
        Map<UUID, String> users = identityFacade.userNames();

        return PostingTimelineEntryResponse.builder()
                .id(posting.getId())
                .detailedPostingId(posting.getId())
                .sourceRecordId(posting.getSourceEntityId())
                .sourceType(posting.getSourceEntityType() == null
                        ? "POSTING" : posting.getSourceEntityType())
                .postingType(posting.getPostingType())
                .postingOrderNumber(posting.getOrderNumber())
                .postingOrderDate(posting.getOrderDate())
                .governmentOrderNumber(posting.getGovernmentOrderNumber())
                .governmentOrderDate(posting.getGovernmentOrderDate())
                .effectiveFrom(posting.getPostingStartDate())
                .effectiveTo(posting.getPostingEndDate())
                .departmentId(posting.getDepartmentId())
                .departmentName(departments.get(posting.getDepartmentId()))
                .sectionId(posting.getSectionId())
                .sectionName(sections.get(posting.getSectionId()))
                .designationId(posting.getDesignationId())
                .designationName(designations.get(posting.getDesignationId()))
                .officeName(posting.getOfficeName())
                .seatName(posting.getSeatName())
                .reportingOfficerName(users.get(posting.getReportingOfficerId()))
                .controllingOfficerName(users.get(posting.getControllingOfficerId()))
                .location(posting.getLocation())
                .district(posting.getDistrict())
                .fromDepartmentName(departments.get(posting.getFromDepartmentId()))
                .fromSectionName(sections.get(posting.getFromSectionId()))
                .fromDesignationName(designations.get(posting.getFromDesignationId()))
                .fromOfficeName(posting.getFromOfficeName())
                .fromLocation(posting.getFromLocation())
                .fromDistrict(posting.getFromDistrict())
                .transferType(posting.getTransferType())
                .transferReason(posting.getTransferReason())
                .permanentCharge(posting.getPermanentCharge())
                .additionalCharge(posting.getAdditionalCharge())
                .currentChargeHolderName(users.get(posting.getCurrentChargeHolderId()))
                .previousChargeHolderName(users.get(posting.getPreviousChargeHolderId()))
                .chargeStartDate(posting.getChargeStartDate())
                .chargeEndDate(posting.getChargeEndDate())
                .chargeStatus(posting.getChargeStatus())
                .joiningDate(posting.getJoiningDate())
                .joiningTime(posting.getJoiningTime())
                .joiningOrder(posting.getJoiningOrder())
                .joiningRemarks(posting.getJoiningRemarks())
                .relievingDate(posting.getRelievingDate())
                .relievingTime(posting.getRelievingTime())
                .relievingOrder(posting.getRelievingOrder())
                .relievingRemarks(posting.getRelievingRemarks())
                .status(posting.getStatus())
                .remarks(posting.getRemarks())
                .primaryAttachmentId(posting.getAttachmentId())
                .documents(documents(posting))
                .createdBy(posting.getCreatedBy())
                .createdByName(users.get(posting.getCreatedBy()))
                .createdAt(posting.getCreatedAt())
                .build();
    }

    public PostingTimelineEntryResponse fromTransfer(TransferResponse transfer) {
        List<PostingDocumentResponse> documents = document(transfer.getAttachmentId(),
                transfer.getOrderDocumentName(), transfer.getOrderDocumentMimeType(),
                transfer.getOrderDocumentSize());
        return PostingTimelineEntryResponse.builder()
                .id(transfer.getId())
                .sourceRecordId(transfer.getId())
                .sourceType("TRANSFER")
                .postingType(transfer.getPostingType() == null ? "TRANSFER" : transfer.getPostingType())
                .postingOrderNumber(transfer.getOrderNumber())
                .postingOrderDate(transfer.getOrderDate())
                .governmentOrderNumber(transfer.getGovernmentOrderNumber())
                .governmentOrderDate(transfer.getGovernmentOrderDate())
                .effectiveFrom(transfer.getTransferDate())
                .effectiveTo(transfer.getEffectiveToDate())
                .departmentId(transfer.getToDepartmentId())
                .departmentName(transfer.getToDepartmentName())
                .sectionId(transfer.getToSectionId())
                .sectionName(transfer.getToSectionName())
                .designationId(transfer.getToDesignationId())
                .designationName(transfer.getToDesignationName())
                .officeName(transfer.getToOfficeName())
                .seatName(transfer.getSeatName())
                .reportingOfficerName(transfer.getReportingOfficerName())
                .controllingOfficerName(transfer.getControllingOfficerName())
                .location(transfer.getToLocation())
                .district(transfer.getToDistrict())
                .fromDepartmentName(transfer.getFromDepartmentName())
                .fromSectionName(transfer.getFromSectionName())
                .fromDesignationName(transfer.getFromDesignationName())
                .fromOfficeName(transfer.getFromOfficeName())
                .fromLocation(transfer.getFromLocation())
                .fromDistrict(transfer.getFromDistrict())
                .transferType(transfer.getTransferType())
                .transferReason(transfer.getTransferReason())
                .permanentCharge(Boolean.TRUE)
                .additionalCharge(Boolean.FALSE)
                .status(transfer.getEffectiveToDate() == null
                        || !transfer.getEffectiveToDate().isBefore(LocalDate.now())
                        ? "CURRENT" : "PAST")
                .remarks(transfer.getRemarks())
                .primaryAttachmentId(transfer.getAttachmentId())
                .documents(documents)
                .createdBy(transfer.getCreatedBy())
                .createdByName(transfer.getCreatedByName())
                .createdAt(transfer.getCreatedAt())
                .build();
    }

    public PostingTimelineEntryResponse fromEvent(JoiningRelievingResponse event) {
        boolean joining = "JOINING".equals(event.getEventType());
        List<PostingDocumentResponse> documents = document(event.getAttachmentId(),
                event.getOrderDocumentName(), event.getOrderDocumentMimeType(),
                event.getOrderDocumentSize());
        return PostingTimelineEntryResponse.builder()
                .id(event.getId())
                .sourceRecordId(event.getId())
                .sourceType("JOINING_RELIEVING")
                .postingType(event.getEventType())
                .postingOrderNumber(event.getOrderNumber())
                .postingOrderDate(event.getOrderDate())
                .governmentOrderNumber(event.getGovernmentOrderNumber())
                .governmentOrderDate(event.getGovernmentOrderDate())
                .effectiveFrom(event.getEventDate())
                .effectiveTo(joining ? null : event.getEventDate())
                .departmentId(event.getDepartmentId())
                .departmentName(event.getDepartmentName())
                .sectionId(event.getSectionId())
                .sectionName(event.getSectionName())
                .designationId(event.getDesignationId())
                .designationName(event.getDesignationName())
                .joiningDate(joining ? event.getEventDate() : null)
                .joiningTime(joining ? event.getEventTime() : null)
                .joiningOrder(joining ? event.getOrderNumber() : null)
                .joiningRemarks(joining ? event.getRemarks() : null)
                .relievingDate(joining ? null : event.getEventDate())
                .relievingTime(joining ? null : event.getEventTime())
                .relievingOrder(joining ? null : event.getOrderNumber())
                .relievingRemarks(joining ? null : event.getRemarks())
                .status(joining ? "JOINED" : "RELIEVED")
                .remarks(event.getRemarks())
                .primaryAttachmentId(event.getAttachmentId())
                .documents(documents)
                .createdBy(event.getCreatedBy())
                .createdByName(event.getCreatedByName())
                .createdAt(event.getCreatedAt())
                .build();
    }

    public PostingTimelineEntryResponse fromCharge(ChargeResponse charge, UUID employeeId) {
        LocalDate from = charge.getEffectiveFrom() == null ? null
                : charge.getEffectiveFrom().toLocalDate();
        LocalDate to = charge.getEffectiveTo() == null ? null
                : charge.getEffectiveTo().toLocalDate();
        List<PostingDocumentResponse> documents = charge.getAttachmentId() == null
                ? List.of()
                : List.of(PostingDocumentResponse.builder()
                        .id(charge.getAttachmentId())
                        .fileName(charge.getOrderDocumentName())
                        .mimeType(charge.getOrderDocumentMimeType())
                        .fileSize(charge.getOrderDocumentSize())
                        .documentCategory("GOVERNMENT_ORDER")
                        .build());
        boolean additional = !"FULL_CHARGE".equals(charge.getChargeType());
        return PostingTimelineEntryResponse.builder()
                .id(charge.getId())
                .sourceRecordId(charge.getId())
                .sourceType("CHARGE")
                .postingType(additional ? "ADDITIONAL_CHARGE" : "CHARGE_ASSIGNMENT")
                .postingOrderNumber(charge.getOrderNumber())
                .postingOrderDate(charge.getOrderDate())
                .effectiveFrom(from)
                .effectiveTo(to)
                .departmentId(charge.getDepartmentId())
                .departmentName(charge.getDepartmentName())
                .sectionId(charge.getSectionId())
                .sectionName(charge.getSectionName())
                .permanentCharge(!additional)
                .additionalCharge(additional)
                .currentChargeHolderName(charge.getToUserName())
                .previousChargeHolderName(charge.getFromUserName())
                .chargeStartDate(from)
                .chargeEndDate(to)
                .chargeStatus(charge.isCurrentlyActive() ? "ACTIVE" : charge.getStatus())
                .status(charge.isCurrentlyActive()
                        ? (additional ? "ADDITIONAL_CHARGE" : "CURRENT")
                        : charge.getStatus())
                .remarks(charge.getReason())
                .primaryAttachmentId(charge.getAttachmentId())
                .documents(documents)
                .createdBy(charge.getCreatedBy())
                .createdByName(charge.getCreatedByName())
                .createdAt(charge.getCreatedAt())
                .build();
    }

    private List<PostingDocumentResponse> document(UUID id, String fileName,
                                                   String mimeType, Long fileSize) {
        if (id == null) {
            return List.of();
        }
        return List.of(PostingDocumentResponse.builder()
                .id(id)
                .fileName(fileName)
                .mimeType(mimeType)
                .fileSize(fileSize)
                .documentCategory("GOVERNMENT_ORDER")
                .build());
    }

    private List<PostingDocumentResponse> documents(UserPosting posting) {
        List<PostingDocumentResponse> result = new ArrayList<>();
        if (posting.getAttachmentId() != null) {
            UUID linkedId = "POSTING".equals(posting.getSourceEntityType())
                    || posting.getSourceEntityId() == null
                    ? posting.getId() : posting.getSourceEntityId();
            attachmentStorageService.listFor(ORDER_ENTITY_TYPE, linkedId).stream()
                    .filter(item -> posting.getAttachmentId().equals(item.id()))
                    .findFirst()
                    .map(item -> toDocument(item, "GOVERNMENT_ORDER"))
                    .ifPresent(result::add);
        }
        attachmentStorageService.listFor(SUPPORTING_ENTITY_TYPE, posting.getId()).stream()
                .map(item -> toDocument(item, "SUPPORTING_DOCUMENT"))
                .forEach(result::add);
        return result;
    }

    private PostingDocumentResponse toDocument(
            AttachmentStorageService.StoredAttachment attachment, String category) {
        return PostingDocumentResponse.builder()
                .id(attachment.id())
                .fileName(attachment.originalFileName())
                .mimeType(attachment.mimeType())
                .fileSize(attachment.fileSize())
                .documentCategory(category)
                .build();
    }
}
