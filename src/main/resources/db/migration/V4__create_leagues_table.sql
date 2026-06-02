CREATE TABLE leagues (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    code       CHAR(6)      NOT NULL UNIQUE,
    owner_id   BIGINT       NOT NULL REFERENCES users(id),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leagues_code     ON leagues(code);
CREATE INDEX idx_leagues_owner_id ON leagues(owner_id);
