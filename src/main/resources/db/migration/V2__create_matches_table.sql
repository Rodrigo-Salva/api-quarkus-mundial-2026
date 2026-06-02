CREATE TABLE matches (
    id         BIGSERIAL    PRIMARY KEY,
    home_team  VARCHAR(100) NOT NULL,
    away_team  VARCHAR(100) NOT NULL,
    home_score INTEGER,
    away_score INTEGER,
    status     VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    match_date TIMESTAMP    NOT NULL
);

CREATE INDEX idx_matches_status     ON matches(status);
CREATE INDEX idx_matches_match_date ON matches(match_date);
