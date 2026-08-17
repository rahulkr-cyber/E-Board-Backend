package com.bor.eboard.charge.dto;

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
public class EmployeeHeaderResponse {
    private UUID employeeId;
    private UUID profileAttachmentId;
    private String employeeName;
    private String employeeCode;
    private String designation;
    private String currentDepartment;
    private String currentSection;
    private String mobileNumber;
    private String email;
    private LocalDate dateOfBirth;
    private LocalDate dateOfJoining;
    private LocalDate retirementDate;
    private String currentStatus;
    private String presentPosting;
    private String district;
}
