package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A remarks-only action on a letter: return, close, reopen, or an internal note.
 *
 * <p>Remarks are mandatory in every case — a letter must never change hands or
 *status without a recorded reason, mirroring the rule already enforced on files.
 */
@Data
public class LetterActionRequest {

    @NotBlank(message = "Remarks are required — the reason is recorded in the letter's history")
    private String remarks;
}
