package com.bor.eboard.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String employeeCode;
    private String username;
    private String fullName;
    private String email;
    private String mobile;
    private String status;
    private UUID departmentId;
    private String departmentName;
    private UUID sectionId;
    private String sectionName;
    private UUID designationId;
    private String designationName;
    private LocalDate dateOfBirth;
    private LocalDate dateOfJoining;
    private LocalDate retirementDate;
    private UUID profileAttachmentId;
    private String district;
    private Boolean accountLocked;
    private LocalDateTime lastLoginAt;
    private List<RoleResponse> roles;
    private LocalDateTime createdAt;
}
