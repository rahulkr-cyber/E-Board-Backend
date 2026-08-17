package com.bor.eboard.common.entity;

import com.bor.eboard.common.util.SecurityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Common audit columns shared by master and transactional tables.
 * See 02_ARCHITECTURE.md section 20 and 03_DATABASE.md section 3.
 *
 * Note: no JPA associations are used anywhere in the system.
 * All foreign keys are plain UUID columns.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.deleted == null) {
            this.deleted = Boolean.FALSE;
        }
        if (this.createdBy == null) {
            this.createdBy = SecurityUtils.getCurrentUserId().orElse(null);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = SecurityUtils.getCurrentUserId().orElse(this.updatedBy);
    }
}
