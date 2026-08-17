package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsMetadataConfigurationOptionsResponse;
import com.bor.eboard.dms.dto.DmsMetadataFieldCreateRequest;
import com.bor.eboard.dms.dto.DmsMetadataFieldOrderRequest;
import com.bor.eboard.dms.dto.DmsMetadataFieldResponse;
import com.bor.eboard.dms.dto.DmsMetadataFieldUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface DmsMetadataService {

    DmsMetadataConfigurationOptionsResponse configurationOptions();

    List<DmsMetadataFieldResponse> findAll(UUID documentTypeId, boolean activeOnly);

    DmsMetadataFieldResponse findById(UUID id);

    DmsMetadataFieldResponse create(UUID documentTypeId, DmsMetadataFieldCreateRequest request);

    DmsMetadataFieldResponse update(UUID id, DmsMetadataFieldUpdateRequest request);

    DmsMetadataFieldResponse setActive(UUID id, boolean active);

    List<DmsMetadataFieldResponse> reorder(
            UUID documentTypeId,
            DmsMetadataFieldOrderRequest request);

    void delete(UUID id);
}
