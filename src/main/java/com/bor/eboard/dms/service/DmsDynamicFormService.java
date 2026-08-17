package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsDynamicFormOptionsResponse;
import com.bor.eboard.dms.dto.DmsDynamicFormResponse;

import java.util.Map;
import java.util.UUID;

public interface DmsDynamicFormService {

    DmsDynamicFormResponse getForm(UUID documentTypeId);

    DmsDynamicFormOptionsResponse resolveOptions(
            UUID documentTypeId,
            String fieldKey,
            Map<String, Object> values);
}
