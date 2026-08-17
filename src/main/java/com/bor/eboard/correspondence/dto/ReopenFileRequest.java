package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** BCR-03 Part 8: only Chairman / Commissioner & Secretary may reopen. */
@Data
public class ReopenFileRequest {

    @NotBlank(message = "A reason is required to reopen a file")
    private String reason;
}
