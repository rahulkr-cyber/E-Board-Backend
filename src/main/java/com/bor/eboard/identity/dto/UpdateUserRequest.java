package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must be at most 200 characters")
    private String fullName;

    @Email(message = "Email must be valid")
    @Size(max = 200, message = "Email must be at most 200 characters")
    private String email;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Mobile must be a 10 digit number")
    private String mobile;

    @NotNull(message = "Department is required")
    private UUID departmentId;

    @NotNull(message = "Section is required")
    private UUID sectionId;

    @NotNull(message = "Designation is required")
    private UUID designationId;

    @jakarta.validation.constraints.Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @jakarta.validation.constraints.PastOrPresent(message = "Date of joining cannot be in the future")
    private LocalDate dateOfJoining;

    @Size(max = 150, message = "District must be at most 150 characters")
    private String district;
}
