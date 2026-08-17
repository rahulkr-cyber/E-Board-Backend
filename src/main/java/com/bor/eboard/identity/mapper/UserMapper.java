package com.bor.eboard.identity.mapper;

import com.bor.eboard.identity.dto.RoleResponse;
import com.bor.eboard.identity.dto.UserResponse;
import com.bor.eboard.identity.entity.Department;
import com.bor.eboard.identity.entity.Designation;
import com.bor.eboard.identity.entity.Section;
import com.bor.eboard.identity.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manual mapper. Reference names (department/section/designation/roles)
 * are resolved by the service layer and passed in, because entities hold
 * only UUID foreign keys (07_CLAUDE.md section 4).
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user,
                                   Department department,
                                   Section section,
                                   Designation designation,
                                   List<RoleResponse> roles) {
        return UserResponse.builder()
                .id(user.getId())
                .employeeCode(user.getEmployeeCode())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .status(user.getStatus())
                .departmentId(user.getDepartmentId())
                .departmentName(department != null ? department.getName() : null)
                .sectionId(user.getSectionId())
                .sectionName(section != null ? section.getName() : null)
                .designationId(user.getDesignationId())
                .designationName(designation != null ? designation.getName() : null)
                .dateOfBirth(user.getDateOfBirth())
                .dateOfJoining(user.getDateOfJoining())
                .retirementDate(user.getDateOfBirth() != null
                        ? user.getDateOfBirth().plusYears(60) : null)
                .profileAttachmentId(user.getProfileAttachmentId())
                .district(user.getDistrict())
                .accountLocked(user.getAccountLocked())
                .lastLoginAt(user.getLastLoginAt())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
