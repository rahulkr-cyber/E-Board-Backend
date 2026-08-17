package com.bor.eboard.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {

    private UUID id;
    private UUID departmentId;
    private String departmentName;
    private String code;
    private String name;
    private String description;
    private Boolean active;
}
