package com.bor.eboard.charge.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.charge.dto.CreateJoiningRelievingRequest;
import com.bor.eboard.charge.dto.JoiningRelievingResponse;
import com.bor.eboard.charge.dto.PostingResponse;
import com.bor.eboard.charge.entity.JoiningRelieving;
import com.bor.eboard.charge.entity.TransferHistory;
import com.bor.eboard.charge.mapper.ChargeMapper;
import com.bor.eboard.charge.repository.JoiningRelievingRepository;
import com.bor.eboard.charge.repository.TransferHistoryRepository;
import com.bor.eboard.charge.service.JoiningRelievingService;
import com.bor.eboard.charge.service.PostingService;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Permanent joining/relieving events integrated into the posting timeline. */
@Service
@RequiredArgsConstructor
public class JoiningRelievingServiceImpl implements JoiningRelievingService {

    private static final String MODULE = "POSTING";
    private static final Set<String> EVENT_TYPES = Set.of("JOINING", "RELIEVING");

    private final JoiningRelievingRepository joiningRepository;
    private final TransferHistoryRepository transferRepository;
    private final ChargeMapper mapper;
    private final IdentityFacade identityFacade;
    private final PostingService postingService;
    private final AuditService auditService;
    private final AttachmentStorageService attachmentStorageService;

    @Override
    @Transactional
    public JoiningRelievingResponse record(CreateJoiningRelievingRequest request) {
        String eventType = request.getEventType() == null
                ? null : request.getEventType().trim().toUpperCase();
        if (!EVENT_TYPES.contains(eventType)) {
            throw new ValidationException("Invalid event type: " + request.getEventType());
        }
        IdentityFacade.UserRef user = identityFacade.findUser(request.getUserId())
                .orElseThrow(() -> new ValidationException("Invalid employee"));

        TransferHistory latestTransfer = transferRepository
                .findFirstByUserIdAndDeletedFalseOrderByTransferDateDesc(user.id())
                .orElse(null);
        boolean relieving = "RELIEVING".equals(eventType);
        UUID inferredDepartmentId = latestTransfer == null ? user.departmentId()
                : relieving ? latestTransfer.getFromDepartmentId()
                : latestTransfer.getToDepartmentId();
        UUID inferredSectionId = latestTransfer == null ? user.sectionId()
                : relieving ? latestTransfer.getFromSectionId()
                : latestTransfer.getToSectionId();
        UUID inferredDesignationId = latestTransfer == null ? user.designationId()
                : relieving ? latestTransfer.getFromDesignationId()
                : latestTransfer.getToDesignationId();
        UUID departmentId = request.getDepartmentId() != null
                ? request.getDepartmentId() : inferredDepartmentId;
        UUID sectionId = request.getSectionId() != null
                ? request.getSectionId() : inferredSectionId;
        UUID designationId = request.getDesignationId() != null
                ? request.getDesignationId() : inferredDesignationId;
        validateOrganization(departmentId, sectionId, designationId);

        JoiningRelieving event = new JoiningRelieving();
        event.setUserId(request.getUserId());
        event.setEventType(eventType);
        event.setDepartmentId(departmentId);
        event.setSectionId(sectionId);
        event.setDesignationId(designationId);
        event.setEventDate(request.getEventDate());
        event.setEventTime(request.getEventTime());
        event.setOrderNumber(request.getOrderNumber());
        event.setOrderDate(request.getOrderDate());
        event.setGovernmentOrderNumber(request.getGovernmentOrderNumber());
        event.setGovernmentOrderDate(request.getGovernmentOrderDate());
        event.setAttachmentId(request.getAttachmentId());
        event.setRemarks(request.getRemarks());
        event = joiningRepository.save(event);

        postingService.applyJoiningRelieving(event);
        if (event.getAttachmentId() != null) {
            postingService.syncSourceAttachment("JOINING_RELIEVING",
                    event.getId(), event.getAttachmentId());
        }

        if ("JOINING".equals(eventType)) {
            identityFacade.updateUserPosting(request.getUserId(), departmentId,
                    sectionId, designationId);
            identityFacade.updateUserStatus(request.getUserId(), "ACTIVE");
        } else if (!isTransferSourceRelieving(latestTransfer, event)
                && samePosting(user, departmentId, sectionId, designationId)) {
            // Relieving an old transfer source must not deactivate the employee destination
            // already-created destination posting. Direct/current relieving does.
            identityFacade.updateUserStatus(request.getUserId(), "INACTIVE");
        }

        auditService.record(MODULE, "JOINING_RELIEVING", event.getId(),
                eventType, null,
                "user=" + request.getUserId() + ", date=" + request.getEventDate());
        return mapper.toJoiningRelievingResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoiningRelievingResponse> getByUser(UUID userId) {
        return joiningRepository.findByUserIdOrderByEventDateDesc(userId).stream()
                .map(mapper::toJoiningRelievingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PostingResponse getPostingHistory(UUID userId) {
        return postingService.history(userId);
    }

    @Override
    @Transactional
    public JoiningRelievingResponse uploadOrderDocument(UUID id, MultipartFile file) {
        JoiningRelieving record = joiningRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Joining/relieving event", id));
        UUID previous = record.getAttachmentId();
        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(
                ChargeMapper.ATTACHMENT_ENTITY_TYPE, record.getId(), null, file);
        record.setAttachmentId(stored.id());
        record = joiningRepository.save(record);
        postingService.syncSourceAttachment("JOINING_RELIEVING", record.getId(), stored.id());

        if (previous != null && !previous.equals(stored.id())) {
            attachmentStorageService.deactivate(previous);
        }
        auditService.record(MODULE, "JOINING_RELIEVING", record.getId(),
                previous == null ? "ORDER_DOCUMENT_UPLOAD" : "ORDER_DOCUMENT_REPLACE",
                previous == null ? null : previous.toString(), stored.originalFileName());
        return mapper.toJoiningRelievingResponse(record);
    }

    private boolean isTransferSourceRelieving(TransferHistory transfer,
                                               JoiningRelieving event) {
        return transfer != null
                && "RELIEVING".equals(event.getEventType())
                && !event.getEventDate().isBefore(transfer.getTransferDate())
                && java.util.Objects.equals(event.getDepartmentId(), transfer.getFromDepartmentId())
                && java.util.Objects.equals(event.getSectionId(), transfer.getFromSectionId())
                && java.util.Objects.equals(event.getDesignationId(), transfer.getFromDesignationId());
    }

    private boolean samePosting(IdentityFacade.UserRef user, UUID departmentId,
                                UUID sectionId, UUID designationId) {
        return java.util.Objects.equals(user.departmentId(), departmentId)
                && java.util.Objects.equals(user.sectionId(), sectionId)
                && java.util.Objects.equals(user.designationId(), designationId);
    }

    private void validateOrganization(UUID departmentId, UUID sectionId, UUID designationId) {
        if (departmentId == null || !identityFacade.departmentExists(departmentId)) {
            throw new ValidationException("A valid department is required");
        }
        IdentityFacade.SectionRef section = sectionId == null ? null
                : identityFacade.findSection(sectionId).orElse(null);
        if (section == null || !departmentId.equals(section.departmentId())) {
            throw new ValidationException("Section must belong to the selected department");
        }
        if (designationId == null || !identityFacade.designationExists(designationId)) {
            throw new ValidationException("A valid designation is required");
        }
    }
}
