package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsDocumentTypeCreateRequest;
import com.bor.eboard.dms.dto.DmsDocumentTypeResponse;
import com.bor.eboard.dms.dto.DmsDocumentTypeUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface DmsDocumentTypeService {

    List<DmsDocumentTypeResponse> findAll(boolean activeOnly);

    DmsDocumentTypeResponse findById(UUID id);

    DmsDocumentTypeResponse create(DmsDocumentTypeCreateRequest request);

    DmsDocumentTypeResponse update(UUID id, DmsDocumentTypeUpdateRequest request);

    DmsDocumentTypeResponse setActive(UUID id, boolean active);

    void delete(UUID id);
}
