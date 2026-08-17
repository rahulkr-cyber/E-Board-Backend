package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRolesRequest {

    @NotNull(message = "Role ids are required")
    @Size(min = 1, message = "At least one role is required")
    private List<UUID> roleIds;
}
