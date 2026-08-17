package com.bor.eboard.dms.entity;

import com.bor.eboard.common.entity.BaseEntity;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.security.DmsPrincipalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dms_document_permissions")
public class DmsDocumentPermission extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "share_id")
    private UUID shareId;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, length = 30)
    private DmsPrincipalType principalType;

    @Column(name = "principal_id", nullable = false)
    private UUID principalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 30)
    private DmsDocumentAccessLevel accessLevel;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getShareId() { return shareId; }
    public void setShareId(UUID shareId) { this.shareId = shareId; }
    public DmsPrincipalType getPrincipalType() { return principalType; }
    public void setPrincipalType(DmsPrincipalType principalType) { this.principalType = principalType; }
    public UUID getPrincipalId() { return principalId; }
    public void setPrincipalId(UUID principalId) { this.principalId = principalId; }
    public DmsDocumentAccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(DmsDocumentAccessLevel accessLevel) { this.accessLevel = accessLevel; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
