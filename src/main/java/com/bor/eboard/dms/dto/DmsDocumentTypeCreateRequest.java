package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DmsDocumentTypeCreateRequest(
        @NotBlank(message = "Document type code is required")
        @Pattern(
                regexp = "^[A-Za-z0-9_]{2,50}$",
                message = "Document type code must contain only letters, digits and underscores")
        String code,

        @NotBlank(message = "Document type name is required")
        @Size(max = 150, message = "Document type name must be at most 150 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Min(value = 0, message = "Sort order must be zero or greater")
        Integer sortOrder,

        Boolean active) {
}
