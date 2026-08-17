package com.bor.eboard.charge.service;

import com.bor.eboard.charge.dto.CreateTransferRequest;
import org.springframework.web.multipart.MultipartFile;
import com.bor.eboard.charge.dto.TransferResponse;

import java.util.List;
import java.util.UUID;

public interface TransferService {

    TransferResponse record(CreateTransferRequest request);

    List<TransferResponse> getByUser(UUID userId);

    /**
     * Upload — or replace — the Government Order / Office Order PDF for a
     * transfer. Reuses the shared attachment storage, with the same allow-list
     * of file types and size limit as every other upload in the application.
     * An existing document is deactivated and superseded, never deleted.
     */
    TransferResponse uploadOrderDocument(UUID id, MultipartFile file);
}
