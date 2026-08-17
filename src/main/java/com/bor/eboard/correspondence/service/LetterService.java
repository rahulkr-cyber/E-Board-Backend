package com.bor.eboard.correspondence.service;

import com.bor.eboard.correspondence.dto.AttachmentInfo;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.correspondence.dto.CreateLetterRequest;
import com.bor.eboard.correspondence.dto.LetterMovementResponse;
import com.bor.eboard.correspondence.dto.LetterActionRequest;
import com.bor.eboard.correspondence.dto.MarkLetterRequest;
import com.bor.eboard.correspondence.dto.UpdateLetterRequest;
import com.bor.eboard.correspondence.dto.LetterResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Letter service (02_ARCHITECTURE.md section 12).
 * Letters may be inward/outward/internal; a non-draft letter must belong
 * to a file (06_BUSINESS_RULES.md section 5).
 */
public interface LetterService {

    LetterResponse create(CreateLetterRequest request);

    LetterResponse getById(UUID id);

    List<LetterResponse> listByFile(UUID fileId);

    AttachmentInfo uploadAttachment(UUID letterId, UUID documentTypeId, MultipartFile file);

    // =================================================================
    // BCR-03 Part 16: letter ownership. The same model as files —
    // creator-only visibility until explicitly marked.
    // =================================================================

    /** Box may be: drafts, inbox, sent, returned, closed. */
    PageResponse<LetterResponse> box(String box, PaginationRequest pagination);

    /** Mark the letter to a section/role/user. */
    LetterResponse mark(UUID id, MarkLetterRequest request);

    List<LetterMovementResponse> movements(UUID id);

    // =================================================================
    // Letter lifecycle. The movement trail already declared MARK | RETURN |
    // CLOSE | REOPEN; only MARK was wired. These complete it.
    // =================================================================

    /** Correct a letter's metadata. Owner-only, DRAFT-only. */
    LetterResponse update(UUID id, UpdateLetterRequest request);

    /** Mark the letter back to whoever last held it, for correction. */
    LetterResponse returnLetter(UUID id, LetterActionRequest request);

    /** Close the letter. It leaves every inbox. */
    LetterResponse close(UUID id, LetterActionRequest request);

    /** Reopen a closed letter. It returns to the reopener's inbox. */
    LetterResponse reopen(UUID id, LetterActionRequest request);

    /** Record an internal note without moving the letter. */
    LetterMovementResponse addNote(UUID id, LetterActionRequest request);

    /** General search across letters (not the personal boxes). */
    PageResponse<LetterResponse> search(String letterNumber, String subject,
                                        String direction, String letterType,
                                        String status, UUID sectionId, UUID fileId,
                                        UUID checklistTemplateId, String checklistStatus,
                                        Boolean checklistComplete, Boolean missingMandatory,
                                        PaginationRequest pagination);
}
