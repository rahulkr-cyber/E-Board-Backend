package com.bor.eboard.correspondence.service;

import com.bor.eboard.correspondence.dto.CreateFollowupRequest;
import com.bor.eboard.correspondence.dto.FollowupResponse;
import com.bor.eboard.correspondence.dto.UpdateFollowupRequest;

import java.util.List;
import java.util.UUID;

/**
 * Follow-up service (03_DATABASE.md 8.4).
 */
public interface FollowupService {

    FollowupResponse create(UUID fileId, CreateFollowupRequest request);

    List<FollowupResponse> listByFile(UUID fileId);

    FollowupResponse createForLetter(UUID letterId, CreateFollowupRequest request);

    List<FollowupResponse> listByLetter(UUID letterId);

    FollowupResponse update(UUID id, UpdateFollowupRequest request);
}
