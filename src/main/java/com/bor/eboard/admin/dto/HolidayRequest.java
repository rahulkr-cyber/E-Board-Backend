package com.bor.eboard.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayRequest {

    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;

    @NotBlank(message = "Holiday name is required")
    @Size(max = 200, message = "Holiday name must be at most 200 characters")
    private String name;

    @Size(max = 50, message = "Holiday type must be at most 50 characters")
    private String holidayType;

    private Boolean active;
}
