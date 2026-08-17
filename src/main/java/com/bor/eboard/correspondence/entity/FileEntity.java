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
 * correspondence_files table.
 * Statuses: OPEN | IN_PROGRESS | PENDING_APPROVAL | RETURNED | CLOSED | ARCHIVED.
 * File number BOR/FILE/{YEAR}/{SEQUENCE}: system-generated, immutable
 * (03_DATABASE.md section 18, 06_BUSINESS_RULES.md section 4).
 * Named FileEntity to avoid clashing with java.io.File.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "correspondence_files")
public class FileEntity extends BaseEntity {

    @Column(name = "file_number", nullable = false, unique = true, length = 100, updatable = false)
    private String fileNumber;

    @Column(name = "file_year", nullable = false, updatable = false)
    private Integer fileYear;

    @Column(name = "file_sequence", nullable = false, updatable = false)
    private Long fileSequence;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description")
    private String description;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "priority_id")
    private UUID priorityId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "current_owner_id")
    private UUID currentOwnerId;

    @Column(name = "current_section_id")
    private UUID currentSectionId;

    @Column(name = "current_status", nullable = false, length = 50)
    private String currentStatus;

    @Column(name = "opened_date", nullable = false)
    private LocalDate openedDate;

    @Column(name = "closed_date")
    private LocalDate closedDate;

    @Column(name = "archived_date")
    private LocalDate archivedDate;

    @Column(name = "confidential")
    private Boolean confidential = Boolean.FALSE;
}
