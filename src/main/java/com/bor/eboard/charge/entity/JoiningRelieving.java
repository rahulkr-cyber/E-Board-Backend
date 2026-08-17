package com.bor.eboard.charge.entity;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * org_joining_relieving: a permanent, insert-only joining or relieving
 * event in a user's posting history (03_DATABASE.md 5.6).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "org_joining_relieving")
public class JoiningRelieving {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 30, updatable = false)
    private String eventType;

    @Column(name = "department_id", updatable = false)
    private UUID departmentId;

    @Column(name = "section_id", updatable = false)
    private UUID sectionId;

    @Column(name = "designation_id", updatable = false)
    private UUID designationId;

    @Column(name = "event_date", nullable = false, updatable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", updatable = false)
    private LocalTime eventTime;

    @Column(name = "order_number", length = 100, updatable = false)
    private String orderNumber;

    @Column(name = "order_date", updatable = false)
    private LocalDate orderDate;

    @Column(name = "government_order_number", length = 100, updatable = false)
    private String governmentOrderNumber;

    @Column(name = "government_order_date", updatable = false)
    private LocalDate governmentOrderDate;

    @Column(name = "attachment_id")
    private UUID attachmentId;

    @Column(name = "remarks", updatable = false)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.createdBy == null) {
            this.createdBy = SecurityUtils.getCurrentUserId().orElse(null);
        }
    }
}
