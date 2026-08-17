package com.bor.eboard.dms.dto;

import java.util.List;

public record DmsMasterConfigurationOptionsResponse(
        List<CodeLabel> sourceTypes,
        List<CodeLabel> parameterLocations,
        List<CodeLabel> parameterDataTypes,
        List<CodeLabel> httpMethods) {

    public record CodeLabel(String code, String label) {
    }
}
