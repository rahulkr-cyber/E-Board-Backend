package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * PUT /letters/{id} — correct a letter's metadata.
 *
 * <p>Deliberately narrower than {@link CreateLetterRequest}: direction, type and
 * the file it belongs to are fixed at creation and are not updatable here. Only
 * the current owner may update, and only while the letter is still a DRAFT.
 */
@Data
public class UpdateLetterRequest {

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
