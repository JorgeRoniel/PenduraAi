CREATE TABLE refresh_session_tb (
    id UUID PRIMARY KEY,
    user_id INTEGER NOT NULL,
    token_hash VARCHAR(64) NOT NULL ,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_session_user
            FOREIGN KEY (user_id)
            REFERENCES user_tb(id)
            ON DELETE CASCADE,

    CONSTRAINT uk_refresh_session_token_hash
            UNIQUE (token_hash),

    CONSTRAINT ck_refresh_session_token_hash
            CHECK ( token_hash ~ '^[0-9a-f]{64}$' ),

    CONSTRAINT ck_refresh_session_expiration
            CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_session_user_active
    ON refresh_session_tb(user_id)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_refresh_session_expires_at
    ON refresh_session_tb(expires_at);