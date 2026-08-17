package com.bor.eboard.charge.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.charge.dto.CreateTransferRequest;
import com.bor.eboard.charge.dto.TransferResponse;
import com.bor.eboard.charge.entity.TransferHistory;
import com.bor.eboard.charge.mapper.ChargeMapper;
import com.bor.eboard.charge.repository.TransferHistoryRepository;
import com.bor.eboard.charge.repository.UserPostingRepository;
import com.bor.eboard.charge.service.PostingService;
import com.bor.eboard.charge.service.TransferService;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Records permanent transfer history and updates the employee's current
 * posting in the same transaction. Existing workflow ownership is unchanged.
 */
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final String MODULE = "TRANSFER";

    private final TransferHistoryRepository transferRepository;
    private final UserPostingRepository postingRepository;
    private final ChargeMapper mapper;
    private final IdentityFacade identityFacade;
    private final PostingService postingService;
    private final AuditService auditService;
    private final AttachmentStorageService attachmentStorageService;

    @Override
    @Transactional
    public TransferResponse record(CreateTransferRequest request) {
        IdentityFacade.UserRef user = identityFacade.findUser(request.getUserId())
                .orElseThrow(() -> new ValidationException("Invalid employee"));
        IdentityFacade.EmployeeRef employee = identityFacade.findEmployee(request.getUserId())
                .orElseThrow(() -> new ValidationException("Invalid employee"));

        UUID designationId = request.getToDesignationId() != null
                ? request.getToDesignationId() : user.designationId();
        validateOrganization(request.getToDepartmentId(), request.getToSectionId(), designationId);
        if (request.getEffectiveToDate() != null
                && request.getEffectiveToDate().isBefore(request.getTransferDate())) {
            throw new ValidationException(
                    "Effective-to date cannot be before the transfer date");
        }
        validateOfficer(request.getReportingOfficerId(), "reporting officer");
        validateOfficer(request.getControllingOfficerId(), "controlling officer");

        com.bor.eboard.charge.entity.UserPosting currentPosting = postingRepository
                .findByUserIdAndActiveTrueAndDeletedFalseOrderByPostingStartDateDesc(user.id())
                .stream()
                .filter(item -> !Boolean.TRUE.equals(item.getAdditionalCharge()))
                .findFirst()
                .orElse(null);

        TransferHistory transfer = new TransferHistory();
        transfer.setUserId(user.id());
        transfer.setFromDepartmentId(user.departmentId());
        transfer.setFromSectionId(user.sectionId());
        transfer.setFromDesignationId(user.designationId());
        transfer.setToDepartmentId(request.getToDepartmentId());
        transfer.setToSectionId(request.getToSectionId());
        transfer.setToDesignationId(designationId);
        transfer.setPostingType(normalizePostingType(request.getPostingType()));
        transfer.setTransferType(trim(request.getTransferType()));
        transfer.setFromOfficeName(currentPosting == null ? null : currentPosting.getOfficeName());
        transfer.setFromLocation(currentPosting == null ? null : currentPosting.getLocation());
        transfer.setToOfficeName(trim(request.getToOfficeName()));
        transfer.setSeatName(trim(request.getSeatName()));
        transfer.setReportingOfficerId(request.getReportingOfficerId());
        transfer.setControllingOfficerId(request.getControllingOfficerId());
        transfer.setToLocation(trim(request.getToLocation()));
        transfer.setFromDistrict(currentPosting != null
                && StringUtils.hasText(currentPosting.getDistrict())
                ? currentPosting.getDistrict() : employee.district());
        transfer.setToDistrict(trim(request.getToDistrict()));
        transfer.setTransferDate(request.getTransferDate());
        transfer.setEffectiveToDate(request.getEffectiveToDate());
        transfer.setOrderNumber(trim(request.getOrderNumber()));
        transfer.setOrderDate(request.getOrderDate());
        transfer.setGovernmentOrderNumber(trim(request.getGovernmentOrderNumber()));
        transfer.setGovernmentOrderDate(request.getGovernmentOrderDate());
        transfer.setAttachmentId(request.getAttachmentId());
        transfer.setTransferReason(trim(request.getTransferReason()));
        transfer.setRemarks(trim(request.getRemarks()));
        transfer = transferRepository.save(transfer);

        postingService.recordTransferPosting(transfer, user);
        identityFacade.updateUserPosting(user.id(), request.getToDepartmentId(),
                request.getToSectionId(), designationId, request.getToDistrict());
        identityFacade.updateUserStatus(user.id(), "ACTIVE");

        auditService.record(MODULE, "TRANSFER_HISTORY", transfer.getId(), "TRANSFER", null,
                "user=" + user.id() + ", toSection=" + request.getToSectionId());
        return mapper.toTransferResponse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getByUser(UUID userId) {
        return transferRepository.findByUserIdOrderByTransferDateDesc(userId).stream()
                .map(mapper::toTransferResponse)
                .toList();
    }

    @Override
    @Transactional
    public TransferResponse uploadOrderDocument(UUID id, MultipartFile file) {
        TransferHistory record = transferRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer record", id));
        UUID previous = record.getAttachmentId();
        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(
                ChargeMapper.ATTACHMENT_ENTITY_TYPE, record.getId(), null, file);
        record.setAttachmentId(stored.id());
        record = transferRepository.save(record);
        postingService.syncSourceAttachment("TRANSFER", record.getId(), stored.id());

        if (previous != null && !previous.equals(stored.id())) {
            attachmentStorageService.deactivate(previous);
        }
        auditService.record(MODULE, "TRANSFER_HISTORY", record.getId(),
                previous == null ? "ORDER_DOCUMENT_UPLOAD" : "ORDER_DOCUMENT_REPLACE",
                previous == null ? null : previous.toString(), stored.originalFileName());
        return mapper.toTransferResponse(record);
    }

    private void validateOrganization(UUID departmentId, UUID sectionId, UUID designationId) {
        if (departmentId == null || !identityFacade.departmentExists(departmentId)) {
            throw new ValidationException("A valid destination department is required");
        }
        IdentityFacade.SectionRef section = sectionId == null ? null
                : identityFacade.findSection(sectionId).orElse(null);
        if (section == null || !departmentId.equals(section.departmentId())) {
            throw new ValidationException(
                    "Destination section must belong to the selected department");
        }
        if (designationId == null || !identityFacade.designationExists(designationId)) {
            throw new ValidationException("A valid destination designation is required");
        }
    }

    private void validateOfficer(UUID officerId, String label) {
        if (officerId != null && identityFacade.findUser(officerId).isEmpty()) {
            throw new ValidationException("Invalid " + label);
        }
    }

    private String normalizePostingType(String value) {
        if (!StringUtils.hasText(value)) {
            return "TRANSFER";
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
