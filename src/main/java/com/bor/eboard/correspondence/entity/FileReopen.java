package com.bor.eboard.correspondence.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Append-only trail of file reopen events (BCR-03 Part 8). */
@Entity
@Table(name = "file_reopen_history")
@Getter
@Setter
@NoArgsConstructor
public class FileReopen extends BaseEntity {

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "reopened_by", nullable = false)
    private UUID reopenedBy;

    @Column(name = "reopened_at", nullable = false)
    private LocalDateTime reopenedAt;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "previous_status", nullable = false, length = 50)
    private String previousStatus;
}
