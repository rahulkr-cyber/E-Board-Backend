package com.bor.eboard.correspondence.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.correspondence.dto.AttachmentInfo;
import com.bor.eboard.correspondence.dto.ChangePriorityRequest;
import com.bor.eboard.correspondence.dto.CloseFileRequest;
import com.bor.eboard.correspondence.dto.CreateFileRequest;
import com.bor.eboard.correspondence.dto.FileResponse;
import com.bor.eboard.correspondence.dto.MarkFileRequest;
import com.bor.eboard.correspondence.dto.PriorityChangeResponse;
import com.bor.eboard.correspondence.dto.ReopenFileRequest;
import com.bor.eboard.correspondence.dto.ReopenHistoryResponse;
import com.bor.eboard.correspondence.dto.TimelineEntry;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Correspondence / File service (02_ARCHITECTURE.md section 12).
 * File number BOR/FILE/{YEAR}/{SEQ}; closed/archived files are read-only.
 */
public interface FileService {

    FileResponse create(CreateFileRequest request);

    FileResponse getById(UUID id);

    PageResponse<FileResponse> search(String fileNumber, String status,
                                      UUID sectionId, UUID ownerId,
                                      PaginationRequest pagination);

    FileResponse close(UUID id, CloseFileRequest request);

    List<TimelineEntry> timeline(UUID id);

    AttachmentInfo uploadAttachment(UUID fileId, UUID documentTypeId, MultipartFile file);

    // =================================================================
    // BCR-03: ownership boxes, Mark, priority change, reopen.
    // Existing methods above are unchanged (backward compatible).
    // =================================================================

    /** Box may be one of: drafts, inbox, sent, returned, closed (Part 4). */
    PageResponse<FileResponse> box(String box, PaginationRequest pagination);

    /** Mark the file to a specific section/role/user (Part 6). */
    FileResponse mark(UUID id, MarkFileRequest request);

    /** Change file priority — Chairman / Commissioner & Secretary only (Part 7). */
    FileResponse changePriority(UUID id, ChangePriorityRequest request);

    /** Reopen a closed file — Chairman / Commissioner & Secretary only (Part 8). */
    FileResponse reopen(UUID id, ReopenFileRequest request);

    List<PriorityChangeResponse> priorityHistory(UUID id);

    List<ReopenHistoryResponse> reopenHistory(UUID id);
}
