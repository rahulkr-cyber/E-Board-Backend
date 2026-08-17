package com.bor.eboard.identity.mapper;

import com.bor.eboard.identity.dto.DepartmentResponse;
import com.bor.eboard.identity.dto.DesignationResponse;
import com.bor.eboard.identity.dto.SectionResponse;
import com.bor.eboard.identity.entity.Department;
import com.bor.eboard.identity.entity.Designation;
import com.bor.eboard.identity.entity.Section;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .active(department.getActive())
                .build();
    }

    public SectionResponse toResponse(Section section, Department department) {
        return SectionResponse.builder()
                .id(section.getId())
                .departmentId(section.getDepartmentId())
                .departmentName(department != null ? department.getName() : null)
                .code(section.getCode())
                .name(section.getName())
                .description(section.getDescription())
                .active(section.getActive())
                .build();
    }

    public DesignationResponse toResponse(Designation designation) {
        return DesignationResponse.builder()
                .id(designation.getId())
                .code(designation.getCode())
                .name(designation.getName())
                .hierarchyLevel(designation.getHierarchyLevel())
                .active(designation.getActive())
                .build();
    }
}
