package com.bor.eboard.admin.dto;

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
public class PriorityRequest {

    @NotBlank(message = "Priority code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,50}$",
            message = "Priority code must contain only uppercase letters, digits and underscores")
    private String code;

    @NotBlank(message = "Priority name is required")
    @Size(max = 100, message = "Priority name must be at most 100 characters")
    private String name;

    @NotNull(message = "Sort order is required")
    @Min(value = 1, message = "Sort order must be at least 1")
    private Integer sortOrder;

    @Min(value = 0, message = "SLA days cannot be negative")
    private Integer slaDays;

    private Boolean active;
}
