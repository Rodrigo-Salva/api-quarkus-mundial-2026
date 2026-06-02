CREATE TABLE odds_lines (
    id         BIGSERIAL     PRIMARY KEY,
    match_id   BIGINT        NOT NULL REFERENCES matches(id),
    market     VARCHAR(30)   NOT NULL,
    odds       NUMERIC(8,2)  NOT NULL,
    active     BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (match_id, market)
);

CREATE INDEX idx_odds_match_id ON odds_lines(match_id);
