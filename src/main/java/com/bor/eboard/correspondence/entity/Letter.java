package com.bor.eboard.correspondence.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * correspondence_letters table.
 * Directions: INWARD | OUTWARD | INTERNAL.
 * Types: ORIGINAL | REPLY | REMINDER | FOLLOW_UP | NOTE | ORDER.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "correspondence_letters")
public class Letter extends BaseEntity {

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "diary_entry_id")
    private UUID diaryEntryId;

    @Column(name = "letter_direction", nullable = false, length = 20)
    private String letterDirection;

    @Column(name = "letter_type", nullable = false, length = 50)
    private String letterType;

    @Column(name = "letter_number", length = 150)
    private String letterNumber;

    @Column(name = "reference_number", length = 150)
    private String referenceNumber;

    @Column(name = "letter_date")
    private LocalDate letterDate;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "body")
    private String body;

    @Column(name = "sender_name", length = 200)
    private String senderName;

    @Column(name = "sender_designation", length = 200)
    private String senderDesignation;

    @Column(name = "sender_department", length = 200)
    private String senderDepartment;

    @Column(name = "sender_address")
    private String senderAddress;

    @Column(name = "receiver_department_id")
    private UUID receiverDepartmentId;

    @Column(name = "receiver_section_id")
    private UUID receiverSectionId;

    @Column(name = "receiver_user_id")
    private UUID receiverUserId;

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

    @Column(name = "current_owner_id")
    private UUID currentOwnerId;

    @Column(name = "current_section_id")
    private UUID currentSectionId;

    @Column(name = "current_status", nullable = false, length = 50)
    private String currentStatus;
}
