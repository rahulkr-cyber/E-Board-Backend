package com.bor.eboard.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageRequest {

    @NotBlank(message = "Language code is required")
    @Pattern(regexp = "^[A-Z]{2,20}$",
            message = "Language code must contain only uppercase letters")
    private String code;

    @NotBlank(message = "Language name is required")
    @Size(max = 100, message = "Language name must be at most 100 characters")
    private String name;

    private Boolean active;
}
