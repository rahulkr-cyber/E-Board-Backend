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
 * correspondence_dispatch_register table.
 * Dispatch number BOR/DISPATCH/{YEAR}/{SEQUENCE}: system-generated, immutable
 * (03_DATABASE.md section 19).
 * Statuses: PENDING | DISPATCHED | DELIVERED | RETURNED | CANCELLED.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "correspondence_dispatch_register")
public class DispatchRegisterEntry extends BaseEntity {

    @Column(name = "letter_id", nullable = false)
    private UUID letterId;

    /** BCR-03 Part 9: a dispatch may instead be raised against a file. */
    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "dispatch_number", nullable = false, unique = true, length = 100, updatable = false)
    private String dispatchNumber;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "dispatch_mode", length = 50)
    private String dispatchMode;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "recipient_department", length = 200)
    private String recipientDepartment;

    @Column(name = "recipient_address")
    private String recipientAddress;

    @Column(name = "tracking_number", length = 150)
    private String trackingNumber;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "remarks")
    private String remarks;
}
