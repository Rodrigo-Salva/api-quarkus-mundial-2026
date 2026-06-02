CREATE TABLE bets (
    id               BIGSERIAL      PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users(id),
    match_id         BIGINT         NOT NULL REFERENCES matches(id),
    market           VARCHAR(30)    NOT NULL,
    stake            NUMERIC(12,2)  NOT NULL,
    odds             NUMERIC(8,2)   NOT NULL,
    potential_payout NUMERIC(14,2)  NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    settled_at       TIMESTAMP,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE accumulator_bets (
    id               BIGSERIAL      PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users(id),
    total_stake      NUMERIC(12,2)  NOT NULL,
    total_odds       NUMERIC(10,2)  NOT NULL,
    potential_payout NUMERIC(14,2)  NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE accumulator_bet_selections (
    id                 BIGSERIAL    PRIMARY KEY,
    accumulator_bet_id BIGINT       NOT NULL REFERENCES accumulator_bets(id),
    match_id           BIGINT       NOT NULL REFERENCES matches(id),
    market             VARCHAR(30)  NOT NULL,
    odds               NUMERIC(8,2) NOT NULL,
    result             VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_bets_user_id    ON bets(user_id);
CREATE INDEX idx_bets_match_id   ON bets(match_id);
CREATE INDEX idx_bets_status     ON bets(status);
CREATE INDEX idx_accu_user_id    ON accumulator_bets(user_id);
CREATE INDEX idx_accu_sel_bet_id ON accumulator_bet_selections(accumulator_bet_id);
