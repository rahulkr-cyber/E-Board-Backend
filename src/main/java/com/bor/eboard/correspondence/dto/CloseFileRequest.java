package com.bor.eboard.correspondence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Close file (04_API_SPEC.md 7.6).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloseFileRequest {

    private String remarks;
}
