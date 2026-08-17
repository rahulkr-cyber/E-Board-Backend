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
public class LanguageResponse {

    private UUID id;
    private String code;
    private String name;
    private Boolean active;
}
