package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Create letter (04_API_SPEC.md 7.4).
 * Directions: INWARD | OUTWARD | INTERNAL.
 * Types: ORIGINAL | REPLY | REMINDER | FOLLOW_UP | NOTE | ORDER.
 * A letter must belong to a file unless saved as draft
 * (06_BUSINESS_RULES.md section 5 rule 4).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLetterRequest {

    /** Nullable only when saved as a draft. */
    private UUID fileId;

    private boolean draft;

    @NotBlank(message = "Letter direction is required")
    @Pattern(regexp = "INWARD|OUTWARD|INTERNAL",
            message = "Direction must be INWARD, OUTWARD or INTERNAL")
    private String letterDirection;

    @NotBlank(message = "Letter type is required")
    @Pattern(regexp = "ORIGINAL|REPLY|REMINDER|FOLLOW_UP|NOTE|ORDER",
            message = "Type must be one of ORIGINAL, REPLY, REMINDER, FOLLOW_UP, NOTE, ORDER")
    private String letterType;

    @Size(max = 150)
    private String letterNumber;

    @Size(max = 150)
    private String referenceNumber;

    private LocalDate letterDate;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String body;

    @Size(max = 200)
    private String senderName;

    @Size(max = 200)
    private String senderDesignation;

    @Size(max = 200)
    private String senderDepartment;

    private String senderAddress;

    private UUID receiverDepartmentId;

    private UUID receiverSectionId;

    private UUID receiverUserId;

    private UUID categoryId;

    private UUID priorityId;

    private UUID languageId;

    private Boolean confidential;

    private LocalDate dueDate;

    private LocalDate reminderDate;
}
