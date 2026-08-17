package com.bor.eboard.charge.service;

import com.bor.eboard.charge.dto.ChargeResponse;
import com.bor.eboard.charge.dto.CreateChargeRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ChargeService {

    ChargeResponse create(CreateChargeRequest request);

    List<ChargeResponse> getActive();

    List<ChargeResponse> getByUser(UUID userId);

    ChargeResponse cancel(UUID id);

    ChargeResponse expire(UUID id);

    /**
     * Upload (or replace) the Government Order / Office Order PDF for a charge.
     *
     * <p>Reuses the shared attachment storage — the same allow-list of file
     * types and size limit that governs every other upload in the application.
     * If a document is already attached, it is deactivated and superseded, so
     * this single method serves both the initial upload and a replacement.
     */
    ChargeResponse uploadOrderDocument(UUID id, MultipartFile file);
}
