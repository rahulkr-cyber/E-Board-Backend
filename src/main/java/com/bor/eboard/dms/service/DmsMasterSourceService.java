package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsMasterConfigurationOptionsResponse;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.dto.DmsMasterSourceCreateRequest;
import com.bor.eboard.dms.dto.DmsMasterSourceResponse;
import com.bor.eboard.dms.dto.DmsMasterSourceTestResponse;
import com.bor.eboard.dms.dto.DmsMasterSourceUpdateRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DmsMasterSourceService {
    DmsMasterConfigurationOptionsResponse configurationOptions();
    List<DmsMasterSourceResponse> findAll(boolean activeOnly);
    DmsMasterSourceResponse findById(UUID id);
    DmsMasterSourceResponse create(DmsMasterSourceCreateRequest request);
    DmsMasterSourceResponse update(UUID id, DmsMasterSourceUpdateRequest request);
    DmsMasterSourceResponse setActive(UUID id, boolean active);
    void delete(UUID id);
    List<DmsMasterDataOptionResponse> resolve(UUID id, Map<String, Object> parameters);
    DmsMasterSourceTestResponse test(UUID id, Map<String, Object> parameters);
}
