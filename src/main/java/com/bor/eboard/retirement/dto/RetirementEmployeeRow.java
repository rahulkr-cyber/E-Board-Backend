package com.bor.eboard.retirement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetirementEmployeeRow {
    private UUID employeeId;
    private UUID profileAttachmentId;
    private String employeeName;
    private String employeeCode;
    private String designation;
    private String department;
    private String section;
    private String office;
    private String district;
    private LocalDate dateOfBirth;
    private LocalDate retirementDate;
    private long daysRemaining;
    private String employeeStatus;
    private String retirementStatus;
}
