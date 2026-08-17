package com.bor.eboard.charge.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.charge.dto.ChargeResponse;
import com.bor.eboard.charge.dto.CreateChargeRequest;
import com.bor.eboard.charge.entity.ChargeAssignment;
import com.bor.eboard.charge.mapper.ChargeMapper;
import com.bor.eboard.charge.repository.ChargeAssignmentRepository;
import com.bor.eboard.charge.service.ChargeService;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Charge assignment lifecycle (12_BUSINESS_RULES.md section 12). Creating a
 * charge sets it ACTIVE; cancel/expire are the only state transitions. Every
 * action is audited (rule 9), and charge history is never deleted (rule 8).
 */
@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService {

    private static final String MODULE = "CHARGE";
    private static final Set<String> CHARGE_TYPES =
            Set.of("TEMPORARY", "ADDITIONAL", "FULL_CHARGE");

    private final ChargeAssignmentRepository chargeRepository;
    private final ChargeMapper mapper;
    private final IdentityFacade identityFacade;
    private final AuditService auditService;
    private final com.bor.eboard.filestorage.service.AttachmentStorageService attachmentStorageService;

    @Override
    @Transactional
    public ChargeResponse create(CreateChargeRequest request) {
        if (!CHARGE_TYPES.contains(request.getChargeType())) {
            throw new ValidationException("Invalid charge type: " + request.getChargeType());
        }
        if (request.getFromUserId().equals(request.getToUserId())) {
            throw new ValidationException("Charge from-user and to-user must differ");
        }
        if (identityFacade.findUser(request.getFromUserId()).isEmpty()) {
            throw new ValidationException("Invalid from-user");
        }
        if (identityFacade.findUser(request.getToUserId()).isEmpty()) {
            throw new ValidationException("Invalid to-user");
        }
        if (request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new ValidationException("effectiveTo cannot be before effectiveFrom");
        }

        ChargeAssignment charge = new ChargeAssignment();
        charge.setFromUserId(request.getFromUserId());
        charge.setToUserId(request.getToUserId());
        charge.setChargeType(request.getChargeType());
        charge.setDepartmentId(request.getDepartmentId());
        charge.setSectionId(request.getSectionId());
        charge.setEffectiveFrom(request.getEffectiveFrom());
        charge.setEffectiveTo(request.getEffectiveTo());
        charge.setOrderNumber(request.getOrderNumber());
        charge.setOrderDate(request.getOrderDate());
        charge.setApprovedBy(request.getApprovedBy());
        charge.setAttachmentId(request.getAttachmentId());
        charge.setReason(request.getReason());
        charge.setStatus("ACTIVE");
        charge = chargeRepository.save(charge);

        auditService.record(MODULE, "CHARGE_ASSIGNMENT", charge.getId(), "CREATE", null,
                "from=" + charge.getFromUserId() + ", to=" + charge.getToUserId()
                        + ", type=" + charge.getChargeType());
        return mapper.toChargeResponse(charge);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargeResponse> getActive() {
        return chargeRepository
                .findByDeletedFalseAndStatusOrderByEffectiveFromDesc("ACTIVE").stream()
                .map(mapper::toChargeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargeResponse> getByUser(UUID userId) {
        // Charges held by the user (granted TO them).
        return chargeRepository
                .findByToUserIdAndDeletedFalseOrderByEffectiveFromDesc(userId).stream()
                .map(mapper::toChargeResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChargeResponse cancel(UUID id) {
        return transition(id, "CANCELLED", "CANCEL");
    }

    @Override
    @Transactional
    public ChargeResponse expire(UUID id) {
        return transition(id, "EXPIRED", "EXPIRE");
    }

    private ChargeResponse transition(UUID id, String newStatus, String action) {
        ChargeAssignment charge = chargeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge assignment", id));
        if (!"ACTIVE".equals(charge.getStatus())) {
            throw new ValidationException(
                    "Only an ACTIVE charge can be " + newStatus.toLowerCase());
        }
        charge.setStatus(newStatus);
        charge = chargeRepository.save(charge);
        auditService.record(MODULE, "CHARGE_ASSIGNMENT", charge.getId(), action,
                "ACTIVE", newStatus);
        return mapper.toChargeResponse(charge);
    }

    @Override
    @Transactional
    public ChargeResponse uploadOrderDocument(UUID id, org.springframework.web.multipart.MultipartFile file) {
        ChargeAssignment charge = chargeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge assignment", id));

        UUID previous = charge.getAttachmentId();

        // Storage enforces the shared allow-list (pdf, jpg, png, tif…) and the
        // configured size cap — no separate validation is invented here.
        var stored = attachmentStorageService.store(
                ChargeMapper.ATTACHMENT_ENTITY_TYPE, charge.getId(), null, file);

        charge.setAttachmentId(stored.id());
        chargeRepository.save(charge);

        // Replacing: the superseded document is deactivated, never deleted, so
        // the earlier order remains recoverable for audit.
        if (previous != null && !previous.equals(stored.id())) {
            attachmentStorageService.deactivate(previous);
        }

        auditService.record(MODULE, "CHARGE_ASSIGNMENT", charge.getId(),
                previous == null ? "ORDER_DOCUMENT_UPLOAD" : "ORDER_DOCUMENT_REPLACE",
                previous == null ? null : previous.toString(),
                stored.originalFileName());

        return mapper.toChargeResponse(charge);
    }
}
