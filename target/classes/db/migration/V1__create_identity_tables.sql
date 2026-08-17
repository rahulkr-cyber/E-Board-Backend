-- =====================================================================
-- V1: Identity tables (users, roles, permissions, mappings, tokens)
-- Board of Revenue e-Board & Correspondence Management System
-- =====================================================================

CREATE TABLE identity_users (
    id                    UUID PRIMARY KEY,
    employee_code         VARCHAR(50) UNIQUE NOT NULL,
    username              VARCHAR(100) UNIQUE NOT NULL,
    full_name             VARCHAR(200) NOT NULL,
    email                 VARCHAR(200),
    mobile                VARCHAR(20),
    password_hash         TEXT NOT NULL,
    status                VARCHAR(30) NOT NULL,
    department_id         UUID,
    section_id            UUID,
    designation_id        UUID,
    failed_login_attempts INT DEFAULT 0,
    account_locked        BOOLEAN DEFAULT FALSE,
    last_login_at         TIMESTAMP,
    password_changed_at   TIMESTAMP,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP,
    created_by            UUID,
    updated_by            UUID,
    deleted               BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_identity_users_username      ON identity_users(username);
CREATE INDEX idx_identity_users_employee_code ON identity_users(employee_code);
CREATE INDEX idx_identity_users_section       ON identity_users(section_id);
CREATE INDEX idx_identity_users_department    ON identity_users(department_id);
CREATE INDEX idx_identity_users_status        ON identity_users(status);

CREATE TABLE identity_roles (
    id          UUID PRIMARY KEY,
    code        VARCHAR(100) UNIQUE NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE identity_permissions (
    id          UUID PRIMARY KEY,
    code        VARCHAR(150) UNIQUE NOT NULL,
    module      VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE identity_user_roles (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    role_id        UUID NOT NULL,
    active         BOOLEAN DEFAULT TRUE,
    effective_from DATE,
    effective_to   DATE,
    created_at     TIMESTAMP NOT NULL,
    created_by     UUID,
    CONSTRAINT uq_user_roles UNIQUE (user_id, role_id, effective_from)
);

CREATE INDEX idx_user_roles_user ON identity_user_roles(user_id);
CREATE INDEX idx_user_roles_role ON identity_user_roles(role_id);

CREATE TABLE identity_role_permissions (
    id            UUID PRIMARY KEY,
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    created_by    UUID,
    CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role ON identity_role_permissions(role_id);

CREATE TABLE identity_refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked    BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON identity_refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON identity_refresh_tokens(token_hash);
