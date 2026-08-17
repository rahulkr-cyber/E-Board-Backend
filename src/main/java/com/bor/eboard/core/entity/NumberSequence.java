package com.bor.eboard.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * core_number_sequences table: one row per (sequence key, year).
 * Backs concurrency-safe, yearly-resetting number generation for
 * diary/file/dispatch/receipt numbers (03_DATABASE.md sections 17-19).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_number_sequences")
public class NumberSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sequence_key", nullable = false, length = 100)
    private String sequenceKey;

    @Column(name = "sequence_year", nullable = false)
    private Integer sequenceYear;

    @Column(name = "current_value", nullable = false)
    private Long currentValue = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
