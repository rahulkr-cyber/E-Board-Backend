package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** BCR-03 Part 7: only Chairman / Commissioner & Secretary may change priority. */
@Data
public class ChangePriorityRequest {

    @NotNull(message = "priorityId is required")
    private UUID priorityId;

    private String remarks;
}
