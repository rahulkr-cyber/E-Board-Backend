package com.bor.eboard.correspondence.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.correspondence.dto.CreateDispatchRequest;
import com.bor.eboard.correspondence.dto.CreateFollowupRequest;
import com.bor.eboard.correspondence.dto.DispatchTargetResponse;
import com.bor.eboard.correspondence.dto.FollowupResponse;
import com.bor.eboard.correspondence.dto.UpdateFollowupRequest;
import com.bor.eboard.correspondence.dto.DispatchRegisterRow;
import com.bor.eboard.correspondence.dto.DispatchResponse;
import com.bor.eboard.correspondence.dto.UpdateDispatchStatusRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dispatch service (03_DATABASE.md 8.6).
 * Dispatch number BOR/DISPATCH/{YEAR}/{SEQ}; only OUTWARD letters can be dispatched.
 */
public interface DispatchService {

    DispatchResponse create(CreateDispatchRequest request);

    DispatchResponse getById(UUID id);

    DispatchResponse updateStatus(UUID id, UpdateDispatchStatusRequest request);

    PageResponse<DispatchResponse> search(String dispatchNumber, String recipient,
                                          String mode, String status,
                                          LocalDate fromDate, LocalDate toDate,
                                          PaginationRequest pagination);

    List<DispatchRegisterRow> dispatchRegister(LocalDate fromDate, LocalDate toDate);

    // =================================================================
    // BCR-03 Parts 9-10: searchable dispatch target and follow-ups.
    // =================================================================

    /** Search files and letters for the dispatch picker (no UUID typing). */
    List<DispatchTargetResponse> searchTargets(String query);

    List<FollowupResponse> followups(UUID dispatchId);

    FollowupResponse addFollowup(UUID dispatchId, CreateFollowupRequest request);

    FollowupResponse updateFollowup(UUID followupId, UpdateFollowupRequest request);
}
