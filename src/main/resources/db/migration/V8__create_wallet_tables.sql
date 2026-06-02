CREATE TABLE wallets (
    id         BIGSERIAL      PRIMARY KEY,
    user_id    BIGINT         NOT NULL UNIQUE REFERENCES users(id),
    balance    NUMERIC(14,2)  NOT NULL DEFAULT 0.00,
    currency   VARCHAR(3)     NOT NULL DEFAULT 'PEN',
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE wallet_transactions (
    id             BIGSERIAL      PRIMARY KEY,
    wallet_id      BIGINT         NOT NULL REFERENCES wallets(id),
    type           VARCHAR(20)    NOT NULL,
    amount         NUMERIC(14,2)  NOT NULL,
    balance_before NUMERIC(14,2)  NOT NULL,
    balance_after  NUMERIC(14,2)  NOT NULL,
    reference      VARCHAR(100),
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallet_user_id ON wallets(user_id);
CREATE INDEX idx_wt_wallet_id   ON wallet_transactions(wallet_id);
CREATE INDEX idx_wt_reference   ON wallet_transactions(reference);
