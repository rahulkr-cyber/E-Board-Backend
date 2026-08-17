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
public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,100}$",
            message = "Role code must contain only uppercase letters, digits and underscores")
    private String code;

    @NotBlank(message = "Role name is required")
    @Size(max = 150, message = "Role name must be at most 150 characters")
    private String name;

    private String description;
}
