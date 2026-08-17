package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {

    @NotBlank(message = "Department code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,50}$",
            message = "Department code must contain only uppercase letters, digits and underscores")
    private String code;

    @NotBlank(message = "Department name is required")
    @Size(max = 200, message = "Department name must be at most 200 characters")
    private String name;

    private String description;

    private Boolean active;
}
