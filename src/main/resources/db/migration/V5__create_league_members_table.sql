CREATE TABLE league_members (
    id        BIGSERIAL PRIMARY KEY,
    league_id BIGINT    NOT NULL REFERENCES leagues(id),
    user_id   BIGINT    NOT NULL REFERENCES users(id),
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (league_id, user_id)
);

CREATE INDEX idx_league_members_league_id ON league_members(league_id);
CREATE INDEX idx_league_members_user_id   ON league_members(user_id);
