package com.bor.eboard.registry.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * registry_diary_entries table.
 * Statuses: DRAFT | DIARIZED | FORWARDED | RETURNED_FOR_CORRECTION | CANCELLED.
 * The diary number is system-generated, unique per year, immutable
 * (01_PROJECT.md section 9, 03_DATABASE.md section 17).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "registry_diary_entries")
public class DiaryEntry extends BaseEntity {

    @Column(name = "diary_number", nullable = false, unique = true, length = 100, updatable = false)
    private String diaryNumber;

    @Column(name = "diary_year", nullable = false, updatable = false)
    private Integer diaryYear;

    @Column(name = "diary_sequence", nullable = false, updatable = false)
    private Long diarySequence;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "received_mode", nullable = false, length = 50)
    private String receivedMode;

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @Column(name = "received_by", nullable = false)
    private UUID receivedBy;

    @Column(name = "sender_name", length = 200)
    private String senderName;

    @Column(name = "sender_designation", length = 200)
    private String senderDesignation;

    @Column(name = "sender_department", length = 200)
    private String senderDepartment;

    @Column(name = "sender_address")
    private String senderAddress;

    @Column(name = "sender_email", length = 200)
    private String senderEmail;

    @Column(name = "sender_mobile", length = 20)
    private String senderMobile;

    @Column(name = "original_letter_number", length = 150)
    private String originalLetterNumber;

    @Column(name = "reference_number", length = 150)
    private String referenceNumber;

    @Column(name = "letter_date")
    private LocalDate letterDate;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description")
    private String description;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "priority_id")
    private UUID priorityId;

    @Column(name = "language_id")
    private UUID languageId;

    @Column(name = "confidential")
    private Boolean confidential = Boolean.FALSE;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "reminder_date")
    private LocalDate reminderDate;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "physical_copy_received")
    private Boolean physicalCopyReceived = Boolean.TRUE;

    @Column(name = "barcode_value", length = 200)
    private String barcodeValue;

    @Column(name = "qr_code_value")
    private String qrCodeValue;

    @Column(name = "initial_department_id")
    private UUID initialDepartmentId;

    @Column(name = "initial_section_id")
    private UUID initialSectionId;

    @Column(name = "initial_assigned_user_id")
    private UUID initialAssignedUserId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;
}
