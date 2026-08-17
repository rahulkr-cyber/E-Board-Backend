package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignationRequest {

    @NotBlank(message = "Designation code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,50}$",
            message = "Designation code must contain only uppercase letters, digits and underscores")
    private String code;

    @NotBlank(message = "Designation name is required")
    @Size(max = 200, message = "Designation name must be at most 200 characters")
    private String name;

    @NotNull(message = "Hierarchy level is required")
    @Min(value = 1, message = "Hierarchy level must be at least 1")
    private Integer hierarchyLevel;

    private Boolean active;
}
