package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Employee code is required")
    @Size(max = 50, message = "Employee code must be at most 50 characters")
    private String employeeCode;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must be at most 200 characters")
    private String fullName;

    @Email(message = "Email must be valid")
    @Size(max = 200, message = "Email must be at most 200 characters")
    private String email;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Mobile must be a 10 digit number")
    private String mobile;

    /**
     * Password policy (09_SECURITY.md section 9): minimum 8 characters with
     * uppercase, lowercase, number and special character.
     */
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-]).{8,}$",
            message = "Password must be at least 8 characters and contain uppercase, lowercase, number and special character")
    private String password;

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

    @NotNull(message = "At least one role is required")
    @Size(min = 1, message = "At least one role is required")
    private List<UUID> roleIds;
}
