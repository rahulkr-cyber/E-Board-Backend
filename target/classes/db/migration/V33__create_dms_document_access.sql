-- DMS object-level access and sharing. These tables are independent from
-- Diary, Letter, Workflow, Dispatch and existing attachment permissions.

CREATE TABLE dms_document_shares (
    id              UUID PRIMARY KEY,
    document_id     UUID NOT NULL,
    principal_type  VARCHAR(30) NOT NULL,
    principal_id    UUID NOT NULL,
    shared_by       UUID NOT NULL,
    shared_at       TIMESTAMP NOT NULL,
    expires_at      TIMESTAMP,
    share_note      VARCHAR(1000),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at      TIMESTAMP,
    revoked_by      UUID,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_share_document
        FOREIGN KEY (document_id) REFERENCES dms_documents (id),
    CONSTRAINT fk_dms_share_shared_by
        FOREIGN KEY (shared_by) REFERENCES identity_users (id),
    CONSTRAINT fk_dms_share_revoked_by
        FOREIGN KEY (revoked_by) REFERENCES identity_users (id),
    CONSTRAINT ck_dms_share_principal_type
        CHECK (principal_type IN ('USER', 'ROLE', 'DEPARTMENT', 'SECTION')),
    CONSTRAINT ck_dms_share_expiry
        CHECK (expires_at IS NULL OR expires_at > shared_at)
);

CREATE UNIQUE INDEX uq_dms_active_share_principal
    ON dms_document_shares (document_id, principal_type, principal_id)
    WHERE deleted = FALSE AND active = TRUE;

CREATE INDEX idx_dms_share_document
    ON dms_document_shares (document_id, shared_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_share_principal
    ON dms_document_shares (principal_type, principal_id, expires_at)
    WHERE deleted = FALSE AND active = TRUE;

CREATE TABLE dms_document_permissions (
    id              UUID PRIMARY KEY,
    document_id     UUID NOT NULL,
    share_id        UUID,
    principal_type  VARCHAR(30) NOT NULL,
    principal_id    UUID NOT NULL,
    access_level    VARCHAR(30) NOT NULL,
    expires_at      TIMESTAMP,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_permission_document
        FOREIGN KEY (document_id) REFERENCES dms_documents (id),
    CONSTRAINT fk_dms_permission_share
        FOREIGN KEY (share_id) REFERENCES dms_document_shares (id),
    CONSTRAINT ck_dms_permission_principal_type
        CHECK (principal_type IN ('USER', 'ROLE', 'DEPARTMENT', 'SECTION')),
    CONSTRAINT ck_dms_permission_access_level
        CHECK (access_level IN ('VIEW', 'UPDATE', 'DOWNLOAD', 'UPLOAD_VERSION', 'SHARE', 'DELETE'))
);

CREATE UNIQUE INDEX uq_dms_active_document_permission
    ON dms_document_permissions (
        document_id,
        principal_type,
        principal_id,
        access_level
    )
    WHERE deleted = FALSE AND active = TRUE;

CREATE INDEX idx_dms_permission_document
    ON dms_document_permissions (document_id, access_level)
    WHERE deleted = FALSE AND active = TRUE;

CREATE INDEX idx_dms_permission_principal
    ON dms_document_permissions (
        principal_type,
        principal_id,
        access_level,
        expires_at
    )
    WHERE deleted = FALSE AND active = TRUE;

CREATE INDEX idx_dms_permission_share
    ON dms_document_permissions (share_id)
    WHERE deleted = FALSE AND share_id IS NOT NULL;
