package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 150, message = "Role name must be at most 150 characters")
    private String name;

    private String description;

    @NotNull(message = "Active flag is required")
    private Boolean active;
}
