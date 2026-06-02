CREATE TABLE kyc_verifications (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL UNIQUE REFERENCES users(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    document_type   VARCHAR(20) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    verified_at     TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE gambling_limits (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    limit_type  VARCHAR(30)     NOT NULL,
    amount      NUMERIC(12,2)   NOT NULL,
    currency    VARCHAR(3)      NOT NULL DEFAULT 'PEN',
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, limit_type)
);

CREATE TABLE self_exclusions (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    start_date TIMESTAMP   NOT NULL DEFAULT NOW(),
    end_date   TIMESTAMP,
    reason     TEXT,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kyc_user_id        ON kyc_verifications(user_id);
CREATE INDEX idx_limits_user_id     ON gambling_limits(user_id);
CREATE INDEX idx_exclusions_user_id ON self_exclusions(user_id, active);
