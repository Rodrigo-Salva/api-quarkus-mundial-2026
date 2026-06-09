CREATE TABLE tournaments (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    code        CHAR(8)      NOT NULL UNIQUE,
    owner_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE tournament_leagues (
    id            BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT    NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    league_id     BIGINT    NOT NULL REFERENCES leagues(id)     ON DELETE CASCADE,
    joined_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tournament_id, league_id)
);

CREATE INDEX idx_tournaments_code     ON tournaments(code);
CREATE INDEX idx_tournaments_owner    ON tournaments(owner_id);
CREATE INDEX idx_tl_tournament_id     ON tournament_leagues(tournament_id);
CREATE INDEX idx_tl_league_id         ON tournament_leagues(league_id);
