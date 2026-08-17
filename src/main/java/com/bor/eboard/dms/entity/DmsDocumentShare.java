package com.bor.eboard.dms.entity;

import com.bor.eboard.common.entity.BaseEntity;
import com.bor.eboard.dms.security.DmsPrincipalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dms_document_shares")
public class DmsDocumentShare extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, length = 30)
    private DmsPrincipalType principalType;

    @Column(name = "principal_id", nullable = false)
    private UUID principalId;

    @Column(name = "shared_by", nullable = false)
    private UUID sharedBy;

    @Column(name = "shared_at", nullable = false)
    private LocalDateTime sharedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "share_note", length = 1000)
    private String shareNote;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public DmsPrincipalType getPrincipalType() { return principalType; }
    public void setPrincipalType(DmsPrincipalType principalType) { this.principalType = principalType; }
    public UUID getPrincipalId() { return principalId; }
    public void setPrincipalId(UUID principalId) { this.principalId = principalId; }
    public UUID getSharedBy() { return sharedBy; }
    public void setSharedBy(UUID sharedBy) { this.sharedBy = sharedBy; }
    public LocalDateTime getSharedAt() { return sharedAt; }
    public void setSharedAt(LocalDateTime sharedAt) { this.sharedAt = sharedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getShareNote() { return shareNote; }
    public void setShareNote(String shareNote) { this.shareNote = shareNote; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public UUID getRevokedBy() { return revokedBy; }
    public void setRevokedBy(UUID revokedBy) { this.revokedBy = revokedBy; }
}
