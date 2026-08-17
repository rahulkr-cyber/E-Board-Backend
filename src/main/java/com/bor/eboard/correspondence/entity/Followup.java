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
 * correspondence_followups table.
 * Statuses: OPEN | IN_PROGRESS | CLOSED.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "correspondence_followups")
public class Followup extends BaseEntity {

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "letter_id")
    private UUID letterId;

    // BCR-03 Part 10: follow-up tracking on dispatches.
    @Column(name = "dispatch_id")
    private UUID dispatchId;

    @Column(name = "followup_number", length = 50)
    private String followupNumber;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "reminder_date")
    private LocalDate reminderDate;

    @Column(name = "followup_date", nullable = false)
    private LocalDate followupDate;

    @Column(name = "followup_type", length = 50)
    private String followupType;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "next_followup_date")
    private LocalDate nextFollowupDate;

    @Column(name = "reply_received")
    private Boolean replyReceived = Boolean.FALSE;

    @Column(name = "reply_received_date")
    private LocalDate replyReceivedDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status;
}
