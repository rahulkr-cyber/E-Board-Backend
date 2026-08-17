package com.bor.eboard.checklist.service;

import com.bor.eboard.checklist.dto.ChecklistDtos;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistService {
    List<ChecklistDtos.TemplateResponse> getTemplates();
    ChecklistDtos.TemplateResponse getTemplate(UUID id);
    Optional<ChecklistDtos.TemplateResponse> getTemplateForCategory(UUID categoryId);
    ChecklistDtos.TemplateResponse createTemplate(ChecklistDtos.TemplateRequest request);
    ChecklistDtos.TemplateResponse updateTemplate(UUID id, ChecklistDtos.TemplateRequest request);
    void deleteTemplate(UUID id);

    List<ChecklistDtos.MappingResponse> getMappings();
    ChecklistDtos.MappingResponse saveMapping(ChecklistDtos.MappingRequest request);
    void deleteMapping(UUID categoryId);

    void syncForDiary(UUID diaryId, UUID categoryId);
    void linkDiaryToLetter(UUID diaryId, UUID letterId);

    Optional<ChecklistDtos.InstanceResponse> getForDiary(UUID diaryId);
    Optional<ChecklistDtos.InstanceResponse> getForLetter(UUID letterId);
    Optional<ChecklistDtos.Summary> getSummaryForDiary(UUID diaryId);
    Optional<ChecklistDtos.Summary> getSummaryForLetter(UUID letterId);

    ChecklistDtos.InstanceResponse upload(UUID instanceItemId, MultipartFile file, String remarks);
    ChecklistDtos.InstanceResponse saveResponse(UUID instanceItemId, ChecklistDtos.ResponseRequest request);
    ChecklistDtos.InstanceResponse verify(UUID instanceItemId, ChecklistDtos.VerificationRequest request);
    ChecklistDtos.InstanceResponse requestAdditionalItem(UUID instanceId,
                                                          ChecklistDtos.AdditionalItemRequest request);

    void validateDiaryForward(UUID diaryId, boolean override, String overrideRemarks);
    void validateLetterForward(UUID letterId, boolean override, String overrideRemarks);

    List<UUID> matchingDiaryIds(UUID templateId, String status, Boolean complete,
                                Boolean missingMandatory);
    List<UUID> matchingLetterIds(UUID templateId, String status, Boolean complete,
                                 Boolean missingMandatory);

    ChecklistDtos.DashboardSummary dashboard(UUID ownerId, List<UUID> sectionIds,
                                              boolean organizationWide);
}
