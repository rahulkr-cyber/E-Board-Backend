package com.bor.eboard.documentviewer.service;

import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.entity.Attachment;
import com.bor.eboard.correspondence.repository.AttachmentRepository;
import com.bor.eboard.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves an attachment back to its owning module and reuses that module's
 * existing RBAC boundary before storage content is returned.
 */
@Service
@RequiredArgsConstructor
public class AttachmentAccessService {

    private static final Set<String> POSTING_TYPES = Set.of(
            "POSTING_ORDER", "POSTING_SUPPORTING", "CHARGE", "TRANSFER");

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Attachment requireView(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findByIdAndDeletedFalse(attachmentId)
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
        requireOwningModuleAccess(attachment);
        return attachment;
    }

    private void requireOwningModuleAccess(Attachment attachment) {
        if (attachment.getChecklistInstanceItemId() != null && hasAny(
                "CHECKLIST_VIEW", "CHECKLIST_UPLOAD", "CHECKLIST_VERIFY", "CHECKLIST_ADMIN")) {
            return;
        }
        String linkedType = attachment.getLinkedEntityType() == null
                ? "" : attachment.getLinkedEntityType().trim().toUpperCase(Locale.ROOT);
        if ("DIARY".equals(linkedType)) {
            requireAny("DIARY_VIEW");
        } else if ("LETTER".equals(linkedType)) {
            requireAny("LETTER_VIEW");
        } else if ("FILE".equals(linkedType)) {
            requireAny("FILE_VIEW");
        } else if ("DISPATCH".equals(linkedType)) {
            requireAny("DISPATCH_VIEW");
        } else if (POSTING_TYPES.contains(linkedType)) {
            requireAny("POSTING_VIEW", "TRANSFER_MANAGE", "CHARGE_MANAGE");
        } else if ("EMPLOYEE_PROFILE".equals(linkedType)) {
            requireProfileAccess(attachment);
        } else {
            throw new ForbiddenException("The attachment owner is not supported by the secure viewer");
        }
    }

    private void requireProfileAccess(Attachment attachment) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        boolean ownProfile = currentUserId != null && userRepository.findByIdAndDeletedFalse(currentUserId)
                .map(user -> attachment.getId().equals(user.getProfileAttachmentId()))
                .orElse(false);
        if (!ownProfile) {
            requireAny("USER_VIEW", "USER_UPDATE", "POSTING_VIEW", "DASHBOARD_VIEW");
        }
    }

    private void requireAny(String... permissions) {
        if (hasAny(permissions)) {
            return;
        }
        throw new ForbiddenException("You do not have access to the attachment's owning record");
    }

    private boolean hasAny(String... permissions) {
        for (String permission : permissions) {
            if (SecurityUtils.hasAuthority(permission)) {
                return true;
            }
        }
        return false;
    }
}
