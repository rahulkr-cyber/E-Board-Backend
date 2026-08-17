package com.bor.eboard.registry.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.registry.dto.AttachmentResponse;
import com.bor.eboard.registry.dto.CreateDiaryEntryRequest;
import com.bor.eboard.registry.dto.DiaryEntryResponse;
import com.bor.eboard.registry.dto.DiaryRegisterRow;
import com.bor.eboard.registry.dto.ForwardDiaryRequest;
import com.bor.eboard.registry.dto.UpdateDiaryEntryRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Registry / Diary service (02_ARCHITECTURE.md section 10).
 * Registry is intake-only: it can never approve, reject or close anything.
 */
public interface DiaryService {

    DiaryEntryResponse create(CreateDiaryEntryRequest request);

    /** Allowed only until first forward. */
    DiaryEntryResponse updateMetadata(UUID id, UpdateDiaryEntryRequest request);

    DiaryEntryResponse getById(UUID id);

    PageResponse<DiaryEntryResponse> search(String diaryNumber, String sender,
                                            String subject, String status, UUID sectionId,
                                            LocalDateTime fromDate, LocalDateTime toDate,
                                            UUID checklistTemplateId, String checklistStatus,
                                            Boolean checklistComplete, Boolean missingMandatory,
                                            PaginationRequest pagination);

    /** Forward to section: status -> FORWARDED, inward letter created via facade. */
    DiaryEntryResponse forward(UUID id, ForwardDiaryRequest request);

    /** Allowed only before forward; a forwarded diary can never be cancelled. */
    DiaryEntryResponse cancel(UUID id, String reason);

    AttachmentResponse uploadAttachment(UUID diaryId, UUID documentTypeId, MultipartFile file);

    List<DiaryRegisterRow> diaryRegister(LocalDate fromDate, LocalDate toDate);
}
