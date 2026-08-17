package com.bor.eboard.correspondence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Row of the Dispatch Register report (11_REPORTS.md section 3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchRegisterRow {

    private UUID id;
    private String dispatchNumber;
    private LocalDate dispatchDate;
    private String letterNumber;
    private String fileNumber;
    private String recipientName;
    private String recipientDepartment;
    private String dispatchMode;
    private String trackingNumber;
    private String status;
}
