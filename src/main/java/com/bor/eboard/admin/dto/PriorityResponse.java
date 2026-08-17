package com.bor.eboard.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorityResponse {

    private UUID id;
    private String code;
    private String name;
    private Integer sortOrder;
    private Integer slaDays;
    private Boolean active;
}
