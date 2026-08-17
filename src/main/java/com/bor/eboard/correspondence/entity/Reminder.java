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
 * correspondence_reminders table.
 * Statuses: DRAFT | SENT | ACKNOWLEDGED | CLOSED.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "correspondence_reminders")
public class Reminder extends BaseEntity {

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "letter_id")
    private UUID letterId;

    @Column(name = "reminder_number", length = 100)
    private String reminderNumber;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Column(name = "reminder_type", length = 50)
    private String reminderType;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "generated_by")
    private UUID generatedBy;

    @Column(name = "status", nullable = false, length = 50)
    private String status;
}
