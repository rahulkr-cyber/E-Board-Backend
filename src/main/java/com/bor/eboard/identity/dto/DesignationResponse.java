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
public class DesignationResponse {

    private UUID id;
    private String code;
    private String name;
    private Integer hierarchyLevel;
    private Boolean active;
}
