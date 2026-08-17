package com.bor.eboard.registry.entity;

import com.bor.eboard.common.util.SecurityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * registry_receipt_register table. Immutable acknowledgement records
 * created at diary registration; backs the printable diary receipt.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "registry_receipt_register")
public class ReceiptRegisterEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "diary_entry_id", nullable = false, updatable = false)
    private UUID diaryEntryId;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 100, updatable = false)
    private String receiptNumber;

    @Column(name = "received_from", length = 200, updatable = false)
    private String receivedFrom;

    @Column(name = "received_by", nullable = false, updatable = false)
    private UUID receivedBy;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "remarks", updatable = false)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.createdBy == null) {
            this.createdBy = SecurityUtils.getCurrentUserId().orElse(null);
        }
    }
}
