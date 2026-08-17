package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionRequest {

    @NotNull(message = "Department is required")
    private UUID departmentId;

    @NotBlank(message = "Section code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,50}$",
            message = "Section code must contain only uppercase letters, digits and underscores")
    private String code;

    @NotBlank(message = "Section name is required")
    @Size(max = 200, message = "Section name must be at most 200 characters")
    private String name;

    private String description;

    private Boolean active;
}
