package com.bor.eboard.admin.mapper;

import com.bor.eboard.admin.dto.DocumentTypeResponse;
import com.bor.eboard.admin.dto.HolidayResponse;
import com.bor.eboard.admin.dto.LanguageResponse;
import com.bor.eboard.admin.dto.LetterCategoryResponse;
import com.bor.eboard.admin.dto.PriorityResponse;
import com.bor.eboard.admin.dto.SystemSettingResponse;
import com.bor.eboard.admin.entity.DocumentType;
import com.bor.eboard.admin.entity.Holiday;
import com.bor.eboard.admin.entity.Language;
import com.bor.eboard.admin.entity.LetterCategory;
import com.bor.eboard.admin.entity.Priority;
import com.bor.eboard.admin.entity.SystemSetting;
import org.springframework.stereotype.Component;

@Component
public class MasterDataMapper {

    public LetterCategoryResponse toResponse(LetterCategory entity) {
        return LetterCategoryResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .defaultWorkflowTemplateId(entity.getDefaultWorkflowTemplateId())
                .active(entity.getActive())
                .build();
    }

    public PriorityResponse toResponse(Priority entity) {
        return PriorityResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .sortOrder(entity.getSortOrder())
                .slaDays(entity.getSlaDays())
                .active(entity.getActive())
                .build();
    }

    public DocumentTypeResponse toResponse(DocumentType entity) {
        return DocumentTypeResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .build();
    }

    public LanguageResponse toResponse(Language entity) {
        return LanguageResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .active(entity.getActive())
                .build();
    }

    public HolidayResponse toResponse(Holiday entity) {
        return HolidayResponse.builder()
                .id(entity.getId())
                .holidayDate(entity.getHolidayDate())
                .name(entity.getName())
                .holidayType(entity.getHolidayType())
                .active(entity.getActive())
                .build();
    }

    public SystemSettingResponse toResponse(SystemSetting entity) {
        return SystemSettingResponse.builder()
                .id(entity.getId())
                .settingKey(entity.getSettingKey())
                .settingValue(entity.getSettingValue())
                .description(entity.getDescription())
                .build();
    }
}
