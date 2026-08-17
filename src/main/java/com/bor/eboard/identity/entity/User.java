package com.bor.eboard.identity.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * identity_users table. Foreign keys are plain UUID columns
 * (no JPA associations, per 07_CLAUDE.md section 4).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "identity_users")
public class User extends BaseEntity {

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "designation_id")
    private UUID designationId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "profile_attachment_id")
    private UUID profileAttachmentId;

    @Column(name = "district", length = 150)
    private String district;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked")
    private Boolean accountLocked = Boolean.FALSE;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
}
