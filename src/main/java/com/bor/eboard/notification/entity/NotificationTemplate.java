package com.bor.eboard.notification.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * notification_templates: reusable title/message templates with
 * {placeholder} tokens resolved at send time.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "title_template", nullable = false)
    private String titleTemplate;

    @Column(name = "message_template", nullable = false)
    private String messageTemplate;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
