package com.bor.eboard.identity.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "identity_authentication_otps")
public class AuthenticationOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false, length = 30)
    private String purpose;
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;
    @Column(name = "mobile_last_four", nullable = false, length = 4)
    private String mobileLastFour;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(nullable = false)
    private Integer attempts = 0;
    @Column(name = "resend_count", nullable = false)
    private Integer resendCount = 0;
    @Column(nullable = false)
    private Boolean verified = Boolean.FALSE;
    @Column(nullable = false)
    private Boolean consumed = Boolean.FALSE;
    @Column(name = "reset_token_hash")
    private String resetTokenHash;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
