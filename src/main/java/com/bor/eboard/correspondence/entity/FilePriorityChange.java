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

/** Append-only trail of file priority changes (BCR-03 Part 7). */
@Entity
@Table(name = "file_priority_change_history")
@Getter
@Setter
@NoArgsConstructor
public class FilePriorityChange extends BaseEntity {

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "old_priority_id")
    private UUID oldPriorityId;

    @Column(name = "new_priority_id", nullable = false)
    private UUID newPriorityId;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}
