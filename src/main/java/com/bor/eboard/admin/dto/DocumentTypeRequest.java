package com.bor.eboard.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeRequest {

    @NotBlank(message = "Document type code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,50}$",
            message = "Document type code must contain only uppercase letters, digits and underscores")
    private String code;

    @NotBlank(message = "Document type name is required")
    @Size(max = 150, message = "Document type name must be at most 150 characters")
    private String name;

    private String description;

    private Boolean active;
}
