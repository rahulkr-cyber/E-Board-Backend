package com.bor.eboard.dms.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsDynamicFormOptionsRequest;
import com.bor.eboard.dms.dto.DmsDynamicFormOptionsResponse;
import com.bor.eboard.dms.dto.DmsDynamicFormResponse;
import com.bor.eboard.dms.service.DmsDynamicFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dms/forms")
@Tag(name = "DMS Dynamic Forms", description = "Metadata-driven DMS runtime forms")
public class DmsDynamicFormController {

    private static final String FORM_AUTHORITIES =
            "hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
                    + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
                    + DmsPermissions.UPDATE + "', '"
                    + DmsPermissions.METADATA_ADMIN + "', '" + DmsPermissions.ADMIN + "')";

    private final DmsDynamicFormService dynamicFormService;

    public DmsDynamicFormController(DmsDynamicFormService dynamicFormService) {
        this.dynamicFormService = dynamicFormService;
    }

    @GetMapping("/{documentTypeId}")
    @PreAuthorize(FORM_AUTHORITIES)
    @Operation(summary = "Get the runtime form definition for a DMS document type")
    public ApiResponse<DmsDynamicFormResponse> getForm(
            @PathVariable UUID documentTypeId) {
        return ApiResponse.success(dynamicFormService.getForm(documentTypeId));
    }

    @PostMapping("/{documentTypeId}/fields/{fieldKey}/options")
    @PreAuthorize(FORM_AUTHORITIES)
    @Operation(summary = "Resolve static, dynamic or cascading options for a form field")
    public ApiResponse<DmsDynamicFormOptionsResponse> resolveOptions(
            @PathVariable UUID documentTypeId,
            @PathVariable String fieldKey,
            @Valid @RequestBody DmsDynamicFormOptionsRequest request) {
        return ApiResponse.success(dynamicFormService.resolveOptions(
                documentTypeId, fieldKey, request.values()));
    }
}
