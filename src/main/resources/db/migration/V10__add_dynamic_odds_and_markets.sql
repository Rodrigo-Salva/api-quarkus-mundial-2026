-- Nuevos campos en odds_lines para soportar cuotas dinámicas
ALTER TABLE odds_lines
    ADD COLUMN IF NOT EXISTS base_odds        NUMERIC(8,2),
    ADD COLUMN IF NOT EXISTS total_staked     NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS bet_count        INTEGER       NOT NULL DEFAULT 0;

-- Tabla de mercados de jugador (jugador que anota, asiste, etc.)
CREATE TABLE IF NOT EXISTS player_markets (
    id           BIGSERIAL    PRIMARY KEY,
    match_id     BIGINT       NOT NULL REFERENCES matches(id),
    player_name  VARCHAR(100) NOT NULL,
    market       VARCHAR(40)  NOT NULL,   -- FIRST_GOAL_SCORER, ANYTIME_SCORER, FIRST_ASSIST
    odds         NUMERIC(8,2) NOT NULL,
    base_odds    NUMERIC(8,2) NOT NULL,
    total_staked NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    bet_count    INTEGER       NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (match_id, player_name, market)
);

CREATE INDEX IF NOT EXISTS idx_player_markets_match_id ON player_markets(match_id);
