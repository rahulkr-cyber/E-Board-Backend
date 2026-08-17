package com.bor.eboard.notification.entity;

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
 * notification_notifications: an in-app notification for a single user.
 * Insert-then-mark-read only; no general update lifecycle.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 255, updatable = false)
    private String title;

    @Column(name = "message", nullable = false, updatable = false)
    private String message;

    @Column(name = "notification_type", nullable = false, length = 50, updatable = false)
    private String notificationType;

    @Column(name = "priority", length = 50, updatable = false)
    private String priority;

    @Column(name = "linked_entity_type", length = 50, updatable = false)
    private String linkedEntityType;

    @Column(name = "linked_entity_id", updatable = false)
    private UUID linkedEntityId;

    @Column(name = "read_flag", nullable = false)
    private Boolean readFlag = Boolean.FALSE;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "expires_at", updatable = false)
    private LocalDateTime expiresAt;

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
        if (this.readFlag == null) {
            this.readFlag = Boolean.FALSE;
        }
    }
}
