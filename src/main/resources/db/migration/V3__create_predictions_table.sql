CREATE TABLE predictions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id),
    match_id   BIGINT    NOT NULL REFERENCES matches(id),
    home_score INTEGER   NOT NULL,
    away_score INTEGER   NOT NULL,
    points     INTEGER   NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, match_id)
);

CREATE INDEX idx_predictions_user_id  ON predictions(user_id);
CREATE INDEX idx_predictions_match_id ON predictions(match_id);
