package com.bor.eboard.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private UUID id;
    private String username;
    private String fullName;
    private String employeeCode;
    private UUID departmentId;
    private String departmentName;
    private UUID sectionId;
    private String sectionName;
    private UUID designationId;
    private String designationName;
    private List<String> roles;
    private List<String> permissions;
}
