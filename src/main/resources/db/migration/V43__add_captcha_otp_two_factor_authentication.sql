ALTER TABLE identity_users ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMP;

CREATE TABLE identity_authentication_otps (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    otp_hash TEXT NOT NULL,
    mobile_last_four VARCHAR(4) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    resend_count INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    reset_token_hash TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_authentication_otp_user FOREIGN KEY (user_id) REFERENCES identity_users(id)
);

CREATE INDEX idx_authentication_otp_user_purpose ON identity_authentication_otps(user_id, purpose);
CREATE INDEX idx_authentication_otp_expiry ON identity_authentication_otps(expires_at);
