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
public class SystemSettingRequest {

    @NotBlank(message = "Setting key is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,150}$",
            message = "Setting key must contain only uppercase letters, digits and underscores")
    private String settingKey;

    @Size(max = 5000, message = "Setting value must be at most 5000 characters")
    private String settingValue;

    private String description;
}
