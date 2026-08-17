package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsMetadataValidationResponse;

import java.util.Map;
import java.util.UUID;

public interface DmsMetadataValidationService {

    DmsMetadataValidationResponse validate(UUID documentTypeId, Map<String, Object> values);
}
