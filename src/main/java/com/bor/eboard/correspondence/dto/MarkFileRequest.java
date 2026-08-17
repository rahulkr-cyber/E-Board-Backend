package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * BCR-03 Part 6: the Mark action replaces a bare forward. The user picks the
 * target Section, Role and User explicitly; the eligible users are supplied by
 * the assignment rules.
 */
@Data
public class MarkFileRequest {

    @NotNull(message = "toUserId is required")
    private UUID toUserId;

    /** The role under which the target was selected (for rule validation). */
    private UUID toRoleId;

    private UUID toSectionId;

    private String remarks;
}
