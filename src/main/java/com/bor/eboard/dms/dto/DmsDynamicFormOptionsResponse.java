package com.bor.eboard.dms.dto;

import java.util.List;

public record DmsDynamicFormOptionsResponse(
        String fieldKey,
        List<DmsDynamicFormResponse.Option> options) {
}
