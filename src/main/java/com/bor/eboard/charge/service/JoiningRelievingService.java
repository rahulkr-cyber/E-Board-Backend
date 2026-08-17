package com.bor.eboard.charge.service;

import com.bor.eboard.charge.dto.CreateJoiningRelievingRequest;
import org.springframework.web.multipart.MultipartFile;
import com.bor.eboard.charge.dto.JoiningRelievingResponse;
import com.bor.eboard.charge.dto.PostingResponse;

import java.util.List;
import java.util.UUID;

public interface JoiningRelievingService {

    JoiningRelievingResponse record(CreateJoiningRelievingRequest request);

    List<JoiningRelievingResponse> getByUser(UUID userId);

    PostingResponse getPostingHistory(UUID userId);

    /**
     * Upload — or replace — the Government Order / Office Order PDF for a
     * joining / relieving event. Reuses the shared attachment storage, with the same allow-list
     * of file types and size limit as every other upload in the application.
     * An existing document is deactivated and superseded, never deleted.
     */
    JoiningRelievingResponse uploadOrderDocument(UUID id, MultipartFile file);
}
