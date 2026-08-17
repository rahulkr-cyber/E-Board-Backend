package com.bor.eboard.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LetterCategoryRequest {

    @NotBlank(message = "Category code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,50}$",
            message = "Category code must contain only uppercase letters, digits and underscores")
    private String code;

    @NotBlank(message = "Category name is required")
    @Size(max = 150, message = "Category name must be at most 150 characters")
    private String name;

    private String description;

    private UUID defaultWorkflowTemplateId;

    private Boolean active;
}
