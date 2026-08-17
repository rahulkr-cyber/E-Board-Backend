package com.bor.eboard.registry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Create diary entry (04_API_SPEC.md 6.1). Mandatory metadata per
 * 01_PROJECT.md section 9: sender, subject, category, priority,
 * received date, addressed section/department.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiaryEntryRequest {

    @NotBlank(message = "Source type is required")
    @Pattern(regexp = "PHYSICAL|ELECTRONIC",
            message = "Source type must be PHYSICAL or ELECTRONIC")
    private String sourceType;

    @NotBlank(message = "Received mode is required")
    @Pattern(regexp = "HAND_DELIVERY|POST|COURIER|EMAIL|FAX|PORTAL|OTHER",
            message = "Received mode must be one of HAND_DELIVERY, POST, COURIER, EMAIL, FAX, PORTAL, OTHER")
    private String receivedMode;

    @NotNull(message = "Received date is required")
    private LocalDateTime receivedDate;

    @NotBlank(message = "Sender name is required")
    @Size(max = 200, message = "Sender name must be at most 200 characters")
    private String senderName;

    @Size(max = 200)
    private String senderDesignation;

    @Size(max = 200)
    private String senderDepartment;

    private String senderAddress;

    @Email(message = "Sender email must be valid")
    @Size(max = 200)
    private String senderEmail;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Sender mobile must be a 10 digit number")
    private String senderMobile;

    @Size(max = 150)
    private String originalLetterNumber;

    @Size(max = 150)
    private String referenceNumber;

    private LocalDate letterDate;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotNull(message = "Priority is required")
    private UUID priorityId;

    private UUID languageId;

    private Boolean confidential;

    private LocalDate dueDate;

    private LocalDate reminderDate;

    @Min(value = 1, message = "Page count must be at least 1")
    private Integer pageCount;

    private Boolean physicalCopyReceived;

    @NotNull(message = "Initial department is required")
    private UUID initialDepartmentId;

    @NotNull(message = "Initial section is required")
    private UUID initialSectionId;

    private String remarks;
}
