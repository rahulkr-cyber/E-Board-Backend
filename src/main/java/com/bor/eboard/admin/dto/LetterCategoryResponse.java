package com.bor.eboard.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterCategoryResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private UUID defaultWorkflowTemplateId;
    private Boolean active;
}
